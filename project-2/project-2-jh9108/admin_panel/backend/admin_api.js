require('dotenv').config();
const express = require('express');
const cors = require('cors');
const bodyParser = require('body-parser');
const db = require('./db_admin');
const authRoutes = require('./routes/authRoutes');
const apiRoutes = require('./routes/apiRoutes');

const app = express();
const PORT = process.env.ADMIN_PORT || 7777;

// Middleware
app.use(cors());
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));

// Initialize DB connection on startup
(async () => {
  try {
    await db.connect();
  } catch (error) {
    console.error("Failed to connect to the database on startup. Exiting.", error);
    process.exit(1);
  }
})();

// Routes
app.use('/api/auth', authRoutes);
app.use('/api', apiRoutes);

app.get('/', (req, res) => {
  res.send('Admin Panel Backend is running and DB connection attempted!');
});

app.listen(PORT, () => {
  console.log(`Admin Panel API server listening on port ${PORT}`);
});

// Graceful shutdown
process.on('SIGINT', async () => {
  console.log('Admin Panel API server shutting down...');
  try {
    await db.disconnect();
  } catch (error) {
    console.error("Error disconnecting from database during shutdown:", error);
  }
  process.exit(0);
});
