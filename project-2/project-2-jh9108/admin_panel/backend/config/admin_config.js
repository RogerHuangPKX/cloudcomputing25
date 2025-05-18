// admin_config.js
// WARNING: DO NOT commit sensitive credentials directly into your repository.
// Use environment variables in production.

const bcrypt = require('bcryptjs');

// For initial setup, you might generate a hash once and store it
// console.log(bcrypt.hashSync('adminpassword', 10));
// Example hash for 'adminpassword': $2a$10$yourGeneratedHashHere (replace with actual generated hash)

const ADMIN_USERNAME = process.env.ADMIN_USERNAME || 'admin';
// Store the HASHED password. Replace this with the actual hash of your desired password.
// To generate a hash: run `node -e "console.log(require('bcryptjs').hashSync('your_password_here', 10))"` in your terminal
const ADMIN_PASSWORD_HASH =
  process.env.ADMIN_PASSWORD_HASH || '$2b$10$MpglMFmLgKvgnptc9x2Qf.K6utYnTw4NbP7NRgDJXmqGpZ1EU7gJa'; // Updated hash for 'adminpassword'
// const JWT_SECRET = process.env.JWT_SECRET || 'yourSuperSecretKey123!@#'; // Removed
// const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '1h'; // Removed

module.exports = {
  ADMIN_USERNAME,
  ADMIN_PASSWORD_HASH,
  // JWT_SECRET, // Removed
  // JWT_EXPIRES_IN // Removed
};
