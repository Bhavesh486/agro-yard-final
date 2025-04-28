import express from 'express';
import admin from 'firebase-admin';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import dotenv from 'dotenv';
import { v4 as uuidv4 } from 'uuid';
import moment from 'moment-timezone';


dotenv.config();

const app = express();
app.use(cors());
app.use(express.json());

// Initialize Firebase Admin SDK
admin.initializeApp({
  credential: admin.credential.cert({
    projectId: process.env.FIREBASE_PROJECT_ID,
    privateKey: process.env.FIREBASE_PRIVATE_KEY.replace(/\\n/g, '\n'),
    clientEmail: process.env.FIREBASE_CLIENT_EMAIL,
  }),
  databaseURL: process.env.FIREBASE_DATABASE_URL,
});

// Firestore reference
const db = admin.firestore();
let prodId = "";

// Create HTTP server using Express
const server = app.listen(process.env.PORT || 3000, () => {
  console.log(`Server is running on port ${process.env.PORT || 3000}`);
});

// Attach WebSocket server to the HTTP server
const wss = new WebSocketServer({ server });

// Handling WebSocket connections
wss.on('connection', (ws) => {
  console.log('Client connected via WebSocket');

  ws.on('message', (message) => {
    console.log(`Received message: ${message}`);
    ws.send('Bid status updated to active');
  });

  ws.on('close', () => {
    console.log('Client disconnected');
  });
});

// Route to update product bid status
app.post('/updateBidStatus', async (req, res) => {
  try {
    const { productId } = req.body;

    if (!productId) {
      return res.status(400).json({ error: 'productId is required' });
    }

    const productRef = db.collection('products').doc(productId);
    const productSnapshot = await productRef.get();

    if (!productSnapshot.exists) {
      return res.status(404).json({ error: 'Product not found' });
    }

    const productData = productSnapshot.data();

    if (productData.bid_status !== 'Starting') {
      return res.status(400).json({ error: 'Bid status is not Starting' });
    }

              const farmerId = productData.farmerId;
              console.log('farmer id', farmerId)

              const farmerSnapshot = await admin.firestore().collection('users').doc(farmerId).get();
              if (!farmerSnapshot.exists) {
                console.log('Farmer document not found!');
                return;
              }

              const farmerData = farmerSnapshot.data();
              const farmerDistrict = farmerData.district;

          const realtimeDb = admin.database();
          await realtimeDb.ref(`bids/${productId}`).set({
            productId,
            farmerDistrict,
            price: productData.price,
            quantity: productData.quantity,
            image: productData.image_url,
            productName: productData.product_name,
            farmerName: productData.farmer_name,
            status: 'Starting',
            updatedAt: Date.now(),
          });

          console.log(`Realtime Database updated for product ${productId}.`);
          await sendBidNotification(productId, 'starting');

    console.log(`Bid for product ${productId} is starting. Will update to 'active' after 60 seconds.`);

    // Wait for 60 seconds before updating bid status
    setTimeout(async () => {
      await productRef.update({
        bid_status: 'active',
        last_updated: admin.firestore.FieldValue.serverTimestamp(),
      });
      console.log(`Bid for product ${productId} updated to 'active'.`);

      // Send WebSocket message to all connected clients
      wss.clients.forEach((client) => {
        if (client.readyState === client.OPEN) {
          client.send('Bid status updated to active');
        }
      });

      const productSnapshot = await admin.firestore().collection('products').doc(productId).get();
          const productData = productSnapshot.data();

          if (!productData) {
            console.log('Product not found!');
            return;
          }

          const farmerId = productData.farmerId;
          console.log('farmer id', farmerId)

          const farmerSnapshot = await admin.firestore().collection('users').doc(farmerId).get();
          if (!farmerSnapshot.exists) {
            console.log('Farmer document not found!');
            return;
          }

          const farmerData = farmerSnapshot.data();
          const farmerDistrict = farmerData.district;

      const realtimeDb = admin.database();
      await realtimeDb.ref(`bids/${productId}`).update({
        productId,
        farmerDistrict,
        price: productData.price,
        quantity: productData.quantity,
        image: productData.image_url,
        productName: productData.product_name,
        farmerName: productData.farmer_name,
        status: 'active',
        updatedAt: Date.now(),
        endTime: Date.now() + (1 * 60 * 1000),

      });

      console.log(`Realtime Database updated for product ${productId}.`);
      await sendBidNotification(productId, 'active');

    }, 20000); // Updated to 60 seconds

    return res.status(200).json({ message: 'Bid status update scheduled.' });

  } catch (error) {
    console.error('Error updating bid status:', error.message);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

async function sendBidNotification(productId, status) {
  try {
    const productSnapshot = await admin.firestore().collection('products').doc(productId).get();
    const productData = productSnapshot.data();

    if (!productData) {
      console.log('Product not found!');
      return;
    }

    const farmerId = productData.farmerId;
    console.log('farmer id', farmerId)

    const farmerSnapshot = await admin.firestore().collection('users').doc(farmerId).get();
    if (!farmerSnapshot.exists) {
      console.log('Farmer document not found!');
      return;
    }

    const farmerData = farmerSnapshot.data();
    const farmerState = farmerData.state;

    const usersSnapshot = await admin.firestore().collection('users').get();
    const deviceTokens = [];

    usersSnapshot.forEach((userDoc) => {
      const userData = userDoc.data();

      if ( userData.state === farmerState && userData.deviceToken) {
        deviceTokens.push(userData.deviceToken);
      }
    });

    if (deviceTokens.length > 0) {
      sendNotifications(deviceTokens, `A new bid for product ${productId} is now ${status} in your district!`);
    } else {
      console.log('No members found in the same district as the farmer.');
    }

  } catch (error) {
    console.error('Error sending notification:', error.message);
  }
}

function sendNotifications(deviceTokens, message) {
 const multicastMessage = {
   notification: {
     title: 'Bid Status Update',
     body: message,
   },
   android: {
     notification: {
       clickAction: 'OPEN_NOTIFICATION_FRAGMENT',
     },
   },
      data: {
        title: 'Bid Status Update',
        body: message,
        type: 'bid_update',
      },
   tokens: deviceTokens,
 };

    admin.messaging().sendEachForMulticast(multicastMessage)
      .then((response) => {
        console.log('Successfully sent message:', response.successCount, 'messages were sent successfully');
      })
      .catch((error) => {
        console.log('Error sending message:', error);
      });
}
// for unique receipt ids

app.post('/saveReceipt', async (req, res) => {
  try {
    const { productId } = req.body;

    if(prodId === productId) {
    return res.status(200).json({ message: 'Receipts created successfully.' });
    }
    prodId = productId;

    if (!productId) {
      return res.status(400).json({ error: 'productId is required' });
    }

    // Fetch product data
    const productRef = admin.firestore().collection('products').doc(productId);
    const productSnapshot = await productRef.get();
    if (!productSnapshot.exists) {
      return res.status(404).json({ error: 'Product not found' });
    }
    const productData = productSnapshot.data();

    if (!productData.is_sold) {
      return res.status(400).json({ error: 'Product is not marked as sold yet.' });
    }

    // Fetch farmer data
    const farmerSnapshot = await admin.firestore().collection('users').doc(productData.farmerId).get();
    if (!farmerSnapshot.exists) {
      return res.status(404).json({ error: 'Farmer not found' });
    }
    const farmerData = farmerSnapshot.data();

    // Fetch member (buyer) data by matching name
    let memberData = null;
    const usersSnapshot = await admin.firestore().collection('users').where('name', '==', productData.sold_to).get();
    if (!usersSnapshot.empty) {
      memberData = usersSnapshot.docs[0].data();
    }

    const realtimeDb = admin.database();
    const timestampMillis = Date.now();

// Format the timestamp properly
const timestamp = moment(timestampMillis)
  .tz('Asia/Kolkata') // for UTC+5:30
  .format('DD MMMM YYYY [at] hh:mm:ss A [UTC+5:30]');    const totalAmount = productData.sold_amount || (productData.price * productData.quantity);

    // Farmer Receipt
    const receiptsCollection = admin.firestore().collection('receipts');

    const farmerReceiptId = uuidv4();
   await receiptsCollection.doc(farmerReceiptId).set({
      productId: productId,
      productName: productData.product_name,
      quantity: productData.quantity,
      pricePerKg: productData.price,
      totalPrice: totalAmount,
      totalPriceLong: totalAmount,
      formattedTotalPrice: `₹${totalAmount}`,
      farmerId: productData.farmerId,
      farmerName: farmerData.name || '',
      farmerPhone: farmerData.mobile || farmerData.phone || '',
      memberName: memberData ? memberData.name : productData.sold_to,
      memberPhone: memberData ? memberData.mobile : '',
      timestamp: timestamp,
      status: 'completed',
    });

    // Member Receipt (only if member data exists)
    if (memberData) {
      const memberReceiptId = uuidv4();
      await receiptsCollection.doc(memberReceiptId).set({
        productId: productId,
        productName: productData.product_name,
        quantity: productData.quantity,
        pricePerKg: productData.price,
        totalPrice: totalAmount,
        formattedTotalPrice: `₹${totalAmount}`,
        totalPriceLong: totalAmount,
        farmerName: farmerData.name || '',
        farmerPhone: farmerData.mobile || farmerData.phone || '',
        memberId: usersSnapshot.docs[0].id,
        memberName: memberData.name,
        memberPhone: memberData.mobile,
        timestamp: timestamp,
        status: 'completed',
      });
    }

    // Mark product as receipt_created: true
    await productRef.update({ receipt_created: true });

    console.log(`Receipts created successfully for product ${productId}`);
    return res.status(200).json({ message: 'Receipts created successfully.' });

  } catch (error) {
    console.error('Error saving receipts:', error.message);
    return res.status(500).json({ error: 'Internal Server Error' });
  }
});

