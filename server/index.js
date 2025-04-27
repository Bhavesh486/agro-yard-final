import express from 'express';
import admin from 'firebase-admin';
import cors from 'cors';
import { WebSocketServer } from 'ws';
import dotenv from 'dotenv';

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
console.log("tokrn", deviceTokens)
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
