const express = require('express');
const router = express.Router();
const db = require('../db_admin'); // Assuming db_admin.js will export necessary functions
const http = require('http'); // For making requests to user_node

// GET /api/messages - Fetch all messages with pagination and filtering
router.get('/messages', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const filters = {
      room_name: req.query.room_name,
      sender_nickname: req.query.sender_nickname,
      message_content: req.query.message_content,
    };
    // Remove undefined filters
    Object.keys(filters).forEach((key) => filters[key] === undefined && delete filters[key]);

    const messages = await db.getAllMessages(filters, page, limit);
    const totalMessages = await db.getTotalMessagesCount(filters);
    res.json({
      data: messages,
      page,
      limit,
      totalPages: Math.ceil(totalMessages / limit),
      totalMessages,
    });
  } catch (error) {
    console.error('Error fetching messages:', error);
    res.status(500).json({ message: 'Failed to fetch messages', error: error.message });
  }
});

// GET /api/rooms - Fetch all rooms with pagination
router.get('/rooms', async (req, res) => {
  try {
    const page = parseInt(req.query.page) || 1;
    const limit = parseInt(req.query.limit) || 10;
    const rooms = await db.getAllRooms(page, limit);
    const totalRooms = await db.getTotalRoomsCount();
    res.json({
      data: rooms,
      page,
      limit,
      totalPages: Math.ceil(totalRooms / limit),
      totalRooms,
    });
  } catch (error) {
    console.error('Error fetching rooms:', error);
    res.status(500).json({ message: 'Failed to fetch rooms', error: error.message });
  }
});

// DELETE /api/messages/:messageId - Delete a specific message
router.delete('/messages/:messageId', async (req, res) => {
  try {
    const { messageId: messageIdStr } = req.params; // Keep original string for logging if needed
    console.log(`Attempting to delete message with param ID string: "${messageIdStr}"`);

    // Validate if messageIdStr is a positive integer string
    if (!messageIdStr || !/^[1-9]\d*$/.test(messageIdStr)) {
        return res.status(400).json({ message: 'Invalid message ID format. Expected a positive integer.' });
    }

    const messageId = parseInt(messageIdStr, 10); // Convert to number

    // Optional: Additional check if parsing failed (though regex should cover it)
    if (isNaN(messageId)) {
        return res.status(400).json({ message: 'Invalid message ID format. ID is not a number.' });
    }

    const deleted = await db.deleteMessageById(messageId); // Pass the numeric ID
    if (deleted) {
      res.json({ message: 'Message deleted successfully' });
    } else {
      res.status(404).json({ message: 'Message not found or already deleted' });
    }
  } catch (error) {
    console.error('Error deleting message:', error);
    res.status(500).json({ message: 'Failed to delete message', error: error.message });
  }
});

// DELETE /api/rooms/:roomId - Delete a specific room
router.delete('/rooms/:roomId', async (req, res) => {
  try {
    const { roomId: roomIdStr } = req.params;
    console.log(`Attempting to delete room with param ID string: "${roomIdStr}"`);

    // Assuming room_id is also an integer for consistency.
    // If room_id is UUID, this needs to be reverted/adjusted for this route.
    // For now, let's assume it might also be an integer if message_id is.
    if (!roomIdStr || !/^[1-9]\d*$/.test(roomIdStr)) {
        return res.status(400).json({ message: 'Invalid room ID format. Expected a positive integer.' });
    }

    const roomId = parseInt(roomIdStr, 10);

    if (isNaN(roomId)) {
        return res.status(400).json({ message: 'Invalid room ID format. ID is not a number.'});
    }

    const deleted = await db.deleteRoomById(roomId);
    if (deleted) {
      res.json({ message: 'Room and associated messages deleted successfully' });
    } else {
      res.status(404).json({ message: 'Room not found or already deleted' });
    }
  } catch (error) {
    console.error('Error deleting room:', error);
    res.status(500).json({ message: 'Failed to delete room', error: error.message });
  }
});

// --- NEW STATS OVERVIEW ENDPOINT ---
router.get('/stats/overview', async (req, res) => {
    const userNodeStatsUrl = 'http://localhost:4000/api/stats/online-users'; // Assuming user_node is on same machine

    try {
        // 1. Get historical max from admin panel's DB connection
        const historicalMaxFromDB = await db.getSystemStat('max_concurrent_users');

        // 2. Get current online stats from user_node
        const fetchUserNodeStats = () => new Promise((resolve, reject) => {
            http.get(userNodeStatsUrl, (apiRes) => {
                let data = '';
                apiRes.on('data', chunk => data += chunk);
                apiRes.on('end', () => {
                    try {
                        if (apiRes.statusCode >= 200 && apiRes.statusCode < 300) {
                            resolve(JSON.parse(data));
                        } else {
                            console.error(`[Stats API] User node responded with ${apiRes.statusCode}: ${data}`);
                            reject(new Error(`User node error: ${apiRes.statusCode} - ${data.substring(0,100)}`));
                        }
                    } catch (e) {
                        console.error('[Stats API] Error parsing JSON from user_node:', e, 'Data:', data);
                        reject(new Error('Failed to parse stats from user node.'));
                    }
                });
            }).on('error', (err) => {
                console.error('[Stats API] Error fetching stats from user_node:', err);
                reject(new Error('Failed to connect to user node for stats.'));
            });
        });

        const userNodeStats = await fetchUserNodeStats();

        res.json({
            success: true,
            currentOnlineTotal: userNodeStats.currentTotalOnline,
            onlineByRoom: userNodeStats.onlineByRoom,
            // historicalMaxOnline: userNodeStats.historicalMaxOnline, // This comes from user_node's in-memory, might differ slightly from DB due to timing
            historicalMaxFromDB: historicalMaxFromDB, // This is the persisted value admin panel trusts
        });

    } catch (error) {
        console.error('[API /stats/overview] Error fetching stats overview:', error);
        res.status(500).json({ success: false, message: 'Failed to fetch stats overview', error: error.message });
    }
});
// --- END NEW STATS OVERVIEW ENDPOINT ---

module.exports = router;
console.log('Debug: apiRoutes.js fully parsed. Typeof router at export:', typeof router);
