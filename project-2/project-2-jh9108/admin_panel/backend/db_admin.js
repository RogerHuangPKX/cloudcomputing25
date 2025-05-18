const { Pool } = require('pg');

const PG_HOST = process.env.PG_HOST || 'pgm-rj92jtpme8o58980fo.rwlb.rds.aliyuncs.com';
const PG_PORT = process.env.PG_PORT || 5432;
const PG_DATABASE = process.env.PG_DATABASE || 'p2pchat';
const PG_USER = process.env.PG_USER || 'jh9108';
const PG_PASSWORD = process.env.PG_PASSWORD || 'Panyikai0305'; // 在生产环境中强烈建议使用环境变量

let pool;

async function connect() {
  if (pool) return pool; // Return existing pool if already connected

  pool = new Pool({
    user: PG_USER,
    host: PG_HOST,
    database: PG_DATABASE,
    password: PG_PASSWORD,
    port: parseInt(PG_PORT),
    // ssl: {
    //   rejectUnauthorized: false // 根据您的数据库SSL配置调整, 阿里云通常需要
    // },
    max: 10, // Max number of clients in the pool
    idleTimeoutMillis: 30000, // How long a client is allowed to remain idle before being closed
    connectionTimeoutMillis: 2000, // How long to wait for a connection from the pool
  });

  try {
    await pool.query('SELECT NOW()'); // Test connection
    console.log('Admin Panel: Successfully connected to PostgreSQL database.');
    return pool;
  } catch (error) {
    console.error('Admin Panel: Failed to connect to PostgreSQL database:', error);
    pool = null; // Reset pool on connection failure
    throw error; // Re-throw error to be caught by caller
  }
}

async function disconnect() {
  if (pool) {
    await pool.end();
    console.log('Admin Panel: PostgreSQL pool has been closed.');
    pool = null;
  }
}

// Define getPool as a standalone function
function getPool() {
  return pool;
}

// Placeholder for admin-specific DB functions
// async function getAllMessages(filters, page, limit) { ... }
// async function deleteRoom(roomId) { ... }
// async function deleteMessage(messageId) { ... }
// async function findAdminUser(username) { ... } // For auth

async function getAllMessages(filters = {}, page = 1, limit = 10) {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  const offset = (page - 1) * limit;
  // Basic filtering example (can be expanded)
  let query = 'SELECT * FROM chat_messages';
  const queryParams = [];
  let whereClause = '';

  if (filters.room_name) {
    queryParams.push(`%${filters.room_name}%`);
    whereClause += (whereClause ? ' AND ' : ' WHERE ') + `room_name ILIKE $${queryParams.length}`;
  }
  if (filters.sender_nickname) {
    queryParams.push(`%${filters.sender_nickname}%`);
    whereClause +=
      (whereClause ? ' AND ' : ' WHERE ') + `sender_nickname ILIKE $${queryParams.length}`;
  }
  if (filters.message_content) {
    queryParams.push(`%${filters.message_content}%`);
    whereClause +=
      (whereClause ? ' AND ' : ' WHERE ') + `message_content ILIKE $${queryParams.length}`;
  }

  query += whereClause;
  query +=
    ' ORDER BY timestamp DESC LIMIT $' +
    (queryParams.length + 1) +
    ' OFFSET $' +
    (queryParams.length + 2);
  queryParams.push(limit, offset);

  const result = await poolClient.query(query, queryParams);
  return result.rows;
}

async function getTotalMessagesCount(filters = {}) {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  let query = 'SELECT COUNT(*) FROM chat_messages';
  const queryParams = [];
  let whereClause = '';

  if (filters.room_name) {
    queryParams.push(`%${filters.room_name}%`);
    whereClause += (whereClause ? ' AND ' : ' WHERE ') + `room_name ILIKE $${queryParams.length}`;
  }
  if (filters.sender_nickname) {
    queryParams.push(`%${filters.sender_nickname}%`);
    whereClause +=
      (whereClause ? ' AND ' : ' WHERE ') + `sender_nickname ILIKE $${queryParams.length}`;
  }
  if (filters.message_content) {
    queryParams.push(`%${filters.message_content}%`);
    whereClause +=
      (whereClause ? ' AND ' : ' WHERE ') + `message_content ILIKE $${queryParams.length}`;
  }

  query += whereClause;
  const result = await poolClient.query(query, queryParams);
  return parseInt(result.rows[0].count, 10);
}

async function getAllRooms(page = 1, limit = 10) {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  const offset = (page - 1) * limit;
  const query =
    'SELECT room_id, room_name, creator_node_id, description, has_password, created_at FROM chat_rooms ORDER BY created_at DESC LIMIT $1 OFFSET $2';
  const result = await poolClient.query(query, [limit, offset]);
  return result.rows;
}

async function getTotalRoomsCount() {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  const query = 'SELECT COUNT(*) FROM chat_rooms';
  const result = await poolClient.query(query);
  return parseInt(result.rows[0].count, 10);
}

async function deleteMessageById(messageId) {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  const query = 'DELETE FROM chat_messages WHERE message_id = $1 RETURNING *';
  const result = await poolClient.query(query, [messageId]);
  return result.rowCount > 0; // Returns true if a row was deleted
}

async function deleteRoomById(roomId) {
  const poolClient = getPool();
  if (!poolClient) throw new Error('Database not connected');
  // Note: You might want to also delete messages associated with this room, or handle it via CASCADE in DB
  // For now, just deleting the room entry.
  const client = await poolClient.connect();
  try {
    await client.query('BEGIN');
    // First, delete messages from the room
    await client.query(
      'DELETE FROM chat_messages WHERE room_name = (SELECT room_name FROM chat_rooms WHERE room_id = $1)',
      [roomId]
    );
    // Then, delete the room
    const roomDeleteResult = await client.query(
      'DELETE FROM chat_rooms WHERE room_id = $1 RETURNING *',
      [roomId]
    );
    await client.query('COMMIT');
    return roomDeleteResult.rowCount > 0;
  } catch (e) {
    await client.query('ROLLBACK');
    throw e;
  } finally {
    client.release();
  }
}

// --- NEW FUNCTION FOR SYSTEM STATS ---
async function getSystemStat(statName) {
  const poolClient = getPool();
  if (!poolClient) throw new Error("Admin DB: Database not connected");
  try {
    const result = await poolClient.query("SELECT stat_value FROM system_stats WHERE stat_name = $1", [statName]);
    if (result.rows.length > 0) {
      return parseInt(result.rows[0].stat_value, 10);
    }
    // If stat_name is 'max_concurrent_users' and not found, might indicate an issue or first run before user_node initialized it.
    // For other stats, null might be appropriate.
    return statName === 'max_concurrent_users' ? 0 : null;
  } catch (error) {
    console.error(`[Admin DB] Error fetching system stat '${statName}':`, error);
    // Depending on how critical this is, you might throw or return a default.
    // For max_concurrent_users, returning 0 on error is safer for display.
    return statName === 'max_concurrent_users' ? 0 : null;
  }
}
// --- END NEW FUNCTION ---

module.exports = {
  connect,
  disconnect,
  getPool, // Now correctly exports the defined function
  getAllMessages,
  getTotalMessagesCount,
  getAllRooms,
  getTotalRoomsCount,
  deleteMessageById,
  deleteRoomById,
  getSystemStat // Export new function
};
