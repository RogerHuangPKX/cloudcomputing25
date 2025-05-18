const { Pool } = require('pg');

// 数据库连接配置 (后续应考虑使用环境变量)
const dbConfig = {
    user: 'jh9108',
    host: 'pgm-rj92jtpme8o58980fo.rwlb.rds.aliyuncs.com',
    database: 'p2pchat',
    password: 'Panyikai0305',
    port: 5432,
    // 可以添加SSL配置，如果阿里云RDS实例需要的话
    // ssl: {
    //   rejectUnauthorized: false // 根据RDS SSL配置调整
    // }
};

const pool = new Pool(dbConfig);

pool.on('connect', () => {
    console.log('[DB] Connected to PostgreSQL database!');
});

pool.on('error', (err) => {
    console.error('[DB] Unexpected error on idle client', err);
    // 最好在这里处理错误，例如尝试重新连接或退出程序
});

// --- NEW FUNCTIONS FOR SYSTEM STATS ---
async function initializeSystemStats() {
    try {
        // Create system_stats table if it doesn't exist
        await pool.query(`
            CREATE TABLE IF NOT EXISTS system_stats (
                stat_name VARCHAR(255) PRIMARY KEY,
                stat_value INTEGER NOT NULL,
                updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
            );
        `);
        console.log('[DB] system_stats table checked/created.');

        // Ensure max_concurrent_users record exists
        const res = await pool.query("SELECT stat_value FROM system_stats WHERE stat_name = 'max_concurrent_users'");
        if (res.rows.length === 0) {
            await pool.query("INSERT INTO system_stats (stat_name, stat_value) VALUES ('max_concurrent_users', 0)");
            console.log('[DB] Initialized max_concurrent_users in system_stats.');
        }
    } catch (error) {
        console.error('[DB] Error initializing system_stats:', error.stack);
        // Not re-throwing here as it might prevent app startup, but log it seriously.
    }
}

async function getMaxConcurrentUsers() {
    try {
        const result = await pool.query("SELECT stat_value FROM system_stats WHERE stat_name = 'max_concurrent_users'");
        if (result.rows.length > 0) {
            return parseInt(result.rows[0].stat_value, 10);
        }
        return 0; // Default if not found for some reason
    } catch (error) {
        console.error('[DB] Error fetching max_concurrent_users:', error.stack);
        return 0; // Return a safe default on error
    }
}

async function updateMaxConcurrentUsers(newMaxCount) {
    if (typeof newMaxCount !== 'number' || newMaxCount < 0) {
        console.warn('[DB] updateMaxConcurrentUsers called with invalid count:', newMaxCount);
        return;
    }
    const client = await pool.connect();
    try {
        await client.query('BEGIN'); // Start transaction
        const currentMaxRes = await client.query("SELECT stat_value FROM system_stats WHERE stat_name = 'max_concurrent_users' FOR UPDATE");
        let currentMax = 0;
        if (currentMaxRes.rows.length > 0) {
            currentMax = parseInt(currentMaxRes.rows[0].stat_value, 10);
        }

        if (newMaxCount > currentMax) {
            await client.query(
                "UPDATE system_stats SET stat_value = $1, updated_at = CURRENT_TIMESTAMP WHERE stat_name = 'max_concurrent_users'",
                [newMaxCount]
            );
            console.log(`[DB] Updated max_concurrent_users to ${newMaxCount}`);
        }
        await client.query('COMMIT'); // Commit transaction
    } catch (error) {
        await client.query('ROLLBACK'); // Rollback on error
        console.error('[DB] Error updating max_concurrent_users:', error.stack);
    } finally {
        client.release();
    }
}
// --- END NEW FUNCTIONS ---

async function connect() {
    try {
        // 测试连接
        const client = await pool.connect();
        console.log('[DB] Successfully acquired a client from the pool.');
        client.release(); // 立即释放客户端回池中
        return true;
    } catch (error) {
        console.error('[DB] Error connecting to the database:', error.stack);
        // 在实际应用中，可能需要更复杂的重试逻辑或错误处理
        throw error; // 重新抛出错误，让调用者处理
    }
}

async function disconnect() {
    try {
        await pool.end();
        console.log('[DB] PostgreSQL pool has been closed.');
    } catch (error) {
        console.error('[DB] Error closing the database pool:', error.stack);
    }
}

/**
 * Saves a chat message to the database.
 * @param {object} message - Message object.
 * @param {string} message.sender_id - Sender ID.
 * @param {string} [message.sender_nickname] - Sender nickname.
 * @param {string} message.message_content - Message content.
 * @param {string} message.room_name - Name of the room the message belongs to.
 * @param {string} [message.message_type='user_message'] - Message type.
 * @returns {Promise<object>} The inserted message record.
 */
async function saveMessage({ sender_id, sender_nickname, message_content, room_name, message_type = 'user_message' }) {
    if (!sender_id || !message_content || !room_name) {
        throw new Error('[DB] sender_id, message_content, and room_name are required to save a message.');
    }
    const query = `
        INSERT INTO chat_messages (sender_id, sender_nickname, message_content, room_name, message_type)
        VALUES ($1, $2, $3, $4, $5)
        RETURNING *;
    `;
    const values = [sender_id, sender_nickname, message_content, room_name, message_type];

    try {
        const result = await pool.query(query, values);
        console.log('[DB] Message saved successfully:', result.rows[0]);
        return result.rows[0];
    } catch (error) {
        console.error('[DB] Error saving message:', error.stack);
        throw error;
    }
}

/**
 * Gets recent chat messages for a specific room.
 * @param {string} room_name - The name of the room.
 * @param {number} [limit=50] - Maximum number of messages to retrieve.
 * @returns {Promise<Array<object>>} Array of message objects.
 */
async function getRecentMessages(room_name, limit = 50) {
    if (!room_name) {
        throw new Error('[DB] room_name is required to get recent messages.');
    }
    const query = `
        SELECT message_id, sender_id, sender_nickname, message_content, room_name, timestamp, message_type
        FROM chat_messages
        WHERE room_name = $1
        ORDER BY timestamp DESC
        LIMIT $2;
    `;
    const values = [room_name, limit];

    try {
        const result = await pool.query(query, values);
        console.log(`[DB] Fetched ${result.rows.length} recent messages for room '${room_name}'.`);
        return result.rows.reverse(); // To display in chronological order (oldest first)
    } catch (error) {
        console.error(`[DB] Error fetching recent messages for room '${room_name}':`, error.stack);
        throw error;
    }
}

/**
 * Gets a room by name, or creates it if it doesn't exist.
 * @param {object} roomData - Room data.
 * @param {string} roomData.room_name - The name of the room.
 * @param {string} [roomData.creator_node_id] - ID of the node creating the room.
 * @param {string} [roomData.description] - Optional description for the room.
 * @param {boolean} [roomData.has_password=false] - Whether the room is intended to be password protected.
 * @param {string} [roomData.provided_password] - The password string provided by the user trying to join. Added for check.
 * @returns {Promise<object>} The room object from the database.
 */
async function getOrCreateRoom({ room_name, creator_node_id, description = '', has_password = false, provided_password = undefined }) {
    if (!room_name) {
        throw new Error('[DB] room_name is required to get or create a room.');
    }

    // Try to find the room first
    const selectQuery = 'SELECT * FROM chat_rooms WHERE room_name = $1';
    try {
        let result = await pool.query(selectQuery, [room_name]);
        if (result.rows.length > 0) {
            const existingRoom = result.rows[0];
            console.log(`[DB] Room '${room_name}' found. DB record has_password: ${existingRoom.has_password}`);

            // If the room is marked as having a password in DB,
            // and the user did not provide any password string for joining.
            if (existingRoom.has_password && (provided_password === undefined || provided_password === '')) {
                 console.warn(`[DB] Auth Error: Attempt to join password-protected room '${room_name}' without providing a password.`);
                 throw new Error(`Password required to join room '${room_name}'.`);
            }
            // Note: This does not validate if a non-empty provided_password is CORRECT.
            // Correct password validation requires storing and comparing password hashes.
            // The P2P topic (derived from roomName + actual_password_used_for_topic_generation) handles incorrect passwords by segregation.

            return existingRoom;
        }

        // If not found, create it
        // 'has_password' for creation is based on the parameter passed, which server.js derives from !!password
        console.log(`[DB] Room '${room_name}' not found, creating new room. Initial has_password set to: ${has_password}`);
        const insertQuery = `
            INSERT INTO chat_rooms (room_name, creator_node_id, description, has_password)
            VALUES ($1, $2, $3, $4)
            RETURNING *;
        `;
        const insertValues = [room_name, creator_node_id, description, has_password];
        result = await pool.query(insertQuery, insertValues);
        console.log(`[DB] Room '${room_name}' created successfully with has_password: ${result.rows[0].has_password}.`);
        return result.rows[0];
    } catch (error) {
        // Log a cleaner message, error.stack is too verbose for normal flow errors like password required.
        console.error(`[DB] Error in getOrCreateRoom for room '${room_name}': ${error.message}`);
        throw error; // Re-throw to be handled by server.js, which will send a message to client
    }
}

/**
 * Lists public chat rooms (those not marked as password protected).
 * @param {number} [limit=20] - Maximum number of rooms to retrieve.
 * @returns {Promise<Array<object>>} Array of public room objects.
 */
async function listPublicRooms(limit = 20) {
    const query = `
        SELECT room_id, room_name, description, creator_node_id, created_at
        FROM chat_rooms
        WHERE has_password = FALSE
        ORDER BY created_at DESC
        LIMIT $1;
    `;
    try {
        const result = await pool.query(query, [limit]);
        console.log(`[DB] Fetched ${result.rows.length} public rooms.`);
        return result.rows;
    } catch (error) {
        console.error('[DB] Error fetching public rooms:', error.stack);
        throw error;
    }
}

module.exports = {
    connect,
    disconnect,
    saveMessage,
    getRecentMessages,
    getOrCreateRoom,
    listPublicRooms,
    initializeSystemStats,
    getMaxConcurrentUsers,
    updateMaxConcurrentUsers,
    pool
};
