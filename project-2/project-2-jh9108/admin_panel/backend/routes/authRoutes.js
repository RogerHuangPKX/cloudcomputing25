const express = require('express');
const bcrypt = require('bcryptjs');
const { ADMIN_USERNAME, ADMIN_PASSWORD_HASH } = require('../config/admin_config');

const router = express.Router();

// POST /api/auth/login
router.post('/login', (req, res) => {
  console.log('Login attempt received:', req.body); // Log received body
  const { username, password } = req.body;

  if (!username || !password) {
    console.log('Login rejected: Username or password missing');
    return res.status(400).json({ message: 'Username and password are required' });
  }

  console.log(`Comparing received username: "${username}" with ADMIN_USERNAME: "${ADMIN_USERNAME}"`);
  console.log(`Received password: "${password}"`);
  // It's not safe to log the actual password hash from config, but we can log the comparison result.

  const passwordMatches = bcrypt.compareSync(password, ADMIN_PASSWORD_HASH);
  console.log('bcrypt.compareSync result:', passwordMatches);

  if (username === ADMIN_USERNAME && passwordMatches) {
    console.log('Login successful for user:', username);
    res.json({
      success: true,
      user: { username: ADMIN_USERNAME }
    });
  } else {
    console.log('Login failed for user:', username);
    res.status(401).json({ message: 'Invalid username or password' });
  }
});

module.exports = router;
