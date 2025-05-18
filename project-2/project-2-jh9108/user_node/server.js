const express = require('express');
const http = require('http');
const WebSocket = require('ws');
const path = require('path');
const { v4: uuidv4 } = require('uuid'); // 引入 uuid
const crypto = require('crypto'); // <-- 新增：引入 crypto 模块
const dotenv = require('dotenv');
const { GoogleGenAI } = require('@google/genai'); // MODIFIED: Changed to GoogleGenAI
const { P2PNode } = require('./p2p'); // Assuming p2p/index.js exports P2PNode
const db = require('./db'); // Assuming db/index.js exports db functions
const { getMaxConcurrentUsers, updateMaxConcurrentUsers } = require('./db'); // <-- IMPORT NEW DB FUNCTIONS
const bootstrapClient = require('./bootstrap_client');

dotenv.config();

const PORT = process.env.PORT || 4000;
const HOST_IP = '0.0.0.0'; // 监听所有网络接口
const ANNOUNCED_IP = process.env.ANNOUNCED_IP || '127.0.0.1'; // 用户节点通告给引导服务器的 IP
const DEFAULT_NICKNAME = process.env.NICKNAME || 'User';
const BOOTSTRAP_SERVER_URL = process.env.BOOTSTRAP_SERVER_URL || 'http://localhost:3000'; // MODIFIED: Added default value

const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server });

// 全局状态用于在线用户列表
const roomOnlineUsers = new Map(); // Map<roomName, Map<nodeId, {nickname, ws? }>> ws is optional, for direct broadcast if needed
const topicHexToRoomName = new Map(); // Map<topicHex, roomName>

// 在 P2PNode 实例化之前生成 nodeId，以便用于引导服务器注册
let nodeId = uuidv4();
const p2pNode = new P2PNode(); // MODIFIED: Cleaner instantiation

let historicalMaxOnline = 0; // <-- Global variable for historical max

// --- Helper function to update online stats ---
async function updateOnlineStats() {
  let currentTotalOnline = 0;
  roomOnlineUsers.forEach((usersInRoom) => {
    currentTotalOnline += usersInRoom.size;
  });
  console.log(`[Stats] Current total online users: ${currentTotalOnline}`);
  if (currentTotalOnline > historicalMaxOnline) {
    historicalMaxOnline = currentTotalOnline; // Update in-memory immediately
    console.log(`[Stats] New historical max online: ${historicalMaxOnline}`);
  }
  await updateMaxConcurrentUsers(currentTotalOnline); // Update DB (this will also compare with DB's current max)
}
// --- End Helper ---

app.use(express.json());
app.use(express.static(path.join(__dirname, 'public')));

app.get('/api/health', (req, res) => {
  res.status(200).json({
    status: 'UP',
    message: 'User node is running',
    nodeId: nodeId, // 使用生成的 nodeId
    p2pConnections: p2pNode.getConnectedPeerCount(),
    bootstrapNodeInfo: bootstrapClient.getCurrentNodeInfo(),
    currentRoom: p2pNode.currentTopic ? Buffer.from(p2pNode.currentTopic).toString('hex') : null, // <-- 新增：显示当前 P2P Topic (如果已加入)
  });
});

// 新增: 用于测试从引导服务器获取的节点列表
app.get('/api/bootstrap-nodes', (req, res) => {
  try {
    const nodes = bootstrapClient.getNodeList();
    res.status(200).json(nodes);
  } catch (error) {
    console.error('[API /bootstrap-nodes] Error getting node list:', error);
    res.status(500).json({ message: 'Error fetching node list from bootstrap client' });
  }
});

// 新增: API 端点，用于列出公开房间
app.get('/api/rooms/list', async (req, res) => {
  try {
    const rooms = await db.listPublicRooms(50); // Limit to 50 public rooms
    res.status(200).json(rooms);
  } catch (error) {
    console.error('Error fetching public rooms:', error);
    res.status(500).json({ error: 'Failed to fetch public rooms' });
  }
});

// 用于测试P2P广播的端点 - 注意：此端点现在可能需要调整，因为它不感知房间上下文
// 暂时保留，但多房间模式下，通过WebSocket发送消息是主要方式
app.post('/api/test-p2p-broadcast', (req, res) => {
  const messageContent =
    req.body.message || `Test P2P broadcast from ${nodeId} at ${new Date().toLocaleTimeString()}`;
  const nickname = process.env.NICKNAME || `User-${nodeId.substring(0, 6)}`;
  const roomName = req.body.roomName || 'default_test_room'; // 测试时需要指定房间名

  if (!p2pNode.currentTopic) {
    return res
      .status(400)
      .json({ success: false, message: 'Node has not joined a P2P topic/room yet.' });
  }

  const p2pMessagePayload = {
    sender_id: nodeId,
    sender_nickname: nickname,
    message_content: messageContent,
    message_type: 'p2p_test_broadcast',
    timestamp: new Date().toISOString(),
    room_name: roomName, // <--- 新增: 确保P2P消息包含房间名
  };

  try {
    console.log(
      `[API /test-p2p-broadcast] Broadcasting message to room ${roomName}:`,
      p2pMessagePayload
    );
    // 确保广播的消息是字符串
    p2pNode.broadcast(JSON.stringify(p2pMessagePayload));
    res
      .status(200)
      .json({ success: true, message: 'Broadcast initiated.', payload: p2pMessagePayload });
  } catch (error) {
    console.error('[API /test-p2p-broadcast] Error broadcasting message:', error);
    res.status(500).json({ success: false, message: 'Failed to broadcast message.' });
  }
});

// --- NEW API ENDPOINT FOR STATS ---
app.get('/api/stats/online-users', (req, res) => {
  let totalOnline = 0;
  const onlineByRoom = {};
  const knownRooms = p2pNode.getActiveTopics(); // Assuming P2PNode can give this

  knownRooms.forEach((topicHex) => {
    // This mapping from topicHex to roomName is missing here, would need to be stored
    // For simplicity, let's assume roomName is topicHex
    const roomName = topicHexToRoomName.get(topicHex) || topicHex; // Use mapping if available, else use topicHex
    const peers = p2pNode.getPeersInRoom(Buffer.from(topicHex, 'hex')); // MODIFIED: getRoomPeers -> getPeersInRoom
    onlineByRoom[roomName] = peers.map((p) => ({ id: p.nodeId, nickname: p.nickname }));
    // Add self if this node's client is in that room (complex to track if multiple client connections per node)
    // For now, counting distinct peer nodeIds
    totalOnline += new Set(peers.map((p) => p.nodeId)).size;

    // Check if this server's primary client is in this room
    roomOnlineUsers.forEach((usersInRoom) => {
      if (usersInRoom.has(nodeId)) {
        const userData = usersInRoom.get(nodeId);
        if (userData.currentRoom === roomName && !peers.find((p) => p.nodeId === userData.nodeId)) {
          // Add self if not already counted via P2P echo
          if (!onlineByRoom[roomName].find((u) => u.id === userData.nodeId)) {
            onlineByRoom[roomName].unshift({ id: userData.nodeId, nickname: userData.nickname });
            totalOnline++;
          }
        }
      }
    });
  });

  // Simplified total online users - sum of unique peers in rooms this node is aware of.
  // And add any local WebSocket clients if they are not counted as peers.
  // This logic needs refinement based on how `p2pNode.getRoomPeers` and `roomOnlineUsers` are managed.

  db.getMaxConcurrentUsers()
    .then((maxUsers) => {
      res.json({
        currentTotalOnlineUsers: totalOnline, // This is a rough estimate
        onlineUsersByRoom: onlineByRoom,
        historicalMaxOnlineUsers: maxUsers,
      });
    })
    .catch((err) => {
      console.error('Error fetching max concurrent users:', err);
      res.status(500).json({ error: 'Failed to get historical stats' });
    });

  // Potentially update max_concurrent_users
  // db.updateMaxConcurrentUsers(totalOnline); // Be careful with frequent updates
});
// --- END NEW API ENDPOINT ---

// Helper function to generate P2P topic
function generateTopicBuffer(roomName, password = '') {
  const topicString = `${roomName}${password}`;
  return crypto.createHash('sha256').update(topicString).digest();
}

// Helper function to call Gemini API
async function callGeminiService(query) {
  const apiKey = 'AIzaSyDfrAi2lVa1uY2hgZ8WdMKy6sc59pfkPxE';

  try {
    // MODIFIED: Strict matching with test_gemini.ts instantiation
    const genAI = new GoogleGenAI({ apiKey: apiKey });

    console.log(`[AI Service] Sending query to Gemini: "${query.substring(0, 100)}..."`);

    // MODIFIED: Strict matching with test_gemini.ts model usage and call structure
    const modelName = 'gemini-1.0-pro'; // Or 'gemini-2.0-flash' as in test_gemini.ts. Let's use pro for now as it's general purpose.
    // User test_gemini.ts has 'gemini-2.0-flash', let's align strictly.
    const strictModelName = 'gemini-2.0-flash';

    const result = await genAI.models.generateContent({
      model: strictModelName, // Using the model name from test_gemini.ts
      contents: query,
    });

    // Assuming result structure is { text: string | undefined | null, ... } based on test_gemini.ts
    // For robust extraction, we should check result.response.text() if available,
    // but test_gemini.ts directly accesses result.text.
    // Let's try to be robust but also respect the direct access pattern if primary fails.

    let aiResponseText = '';
    if (result && result.response && typeof result.response.text === 'function') {
      aiResponseText = result.response.text();
    } else if (result && typeof result.text === 'string') {
      // Fallback to direct .text access as in test_gemini.ts
      aiResponseText = result.text;
    } else if (
      result &&
      result.response &&
      result.response.candidates &&
      result.response.candidates.length > 0 &&
      result.response.candidates[0].content &&
      result.response.candidates[0].content.parts &&
      result.response.candidates[0].content.parts.length > 0 &&
      typeof result.response.candidates[0].content.parts[0].text === 'string'
    ) {
      // Deeper fallback as was in server.js before
      aiResponseText = result.response.candidates[0].content.parts[0].text;
    }

    if (aiResponseText) {
      console.log('[AI Service] Received response from Gemini.');
      return aiResponseText;
    } else {
      // This fallback might still be useful if .text is empty but other parts exist
      if (
        result &&
        result.response &&
        result.response.candidates &&
        result.response.candidates.length > 0 &&
        result.response.candidates[0].content &&
        result.response.candidates[0].content.parts &&
        result.response.candidates[0].content.parts.length > 0
      ) {
        console.log(
          '[AI Service] Received structured response from Gemini (fallback), extracting text.'
        );
        return result.response.candidates[0].content.parts[0].text;
      } else {
        console.error(
          '[AI Service] Gemini response or text missing. Response:',
          JSON.stringify(result, null, 2)
        );
        return 'AI service returned an empty or unparseable response.';
      }
    }
  } catch (error) {
    console.error('[AI Service] Error calling Gemini API:', error);
    return `Error communicating with AI service: ${error.message}`;
  }
}

// Helper function to generate a unique nickname within a room
function generateUniqueNickname(requestedNickname, existingNicknames) {
  let finalNickname = requestedNickname;
  let counter = 1;
  while (existingNicknames.includes(finalNickname)) {
    finalNickname = `${requestedNickname}#${counter}`;
    counter++;
  }
  return finalNickname;
}

// Store connected WebSocket clients
const clients = new Map(); // Map<WebSocket, {id: string, nickname: string, currentRoom: string | null}>

// WebSocket Server Logic
wss.on('connection', (ws) => {
  const clientId = p2pNode.getNodeId(); // MODIFIED: Use P2P node's public key as ID for this server instance user.
  // If multiple users connect to *this* server, they'd share this ID.
  // A more robust system would assign unique IDs per WebSocket connection.
  // For this project, we assume one main user per user_node instance for the UI.

  clients.set(ws, { id: clientId, nickname: DEFAULT_NICKNAME, currentRoom: null });
  console.log(`Client connected. Assigned ID (server's Node ID): ${clientId}`);

  // Send initial data to the client as per App.vue expectations
  ws.send(
    JSON.stringify({
      type: 'initial_data',
      payload: {
        nodeId: clientId,
        history: [], // Empty history initially; room history sent on join
      },
    })
  );

  ws.on('message', async (message) => {
    let parsedMessage;
    try {
      parsedMessage = JSON.parse(message);
    } catch (error) {
      console.error('Failed to parse message:', message, error);
      ws.send(JSON.stringify({ type: 'error', payload: { message: 'Invalid message format.' } }));
      return;
    }

    const clientData = clients.get(ws);
    console.log(`Received message from client (${clientData.nickname}):`, parsedMessage);

    switch (parsedMessage.type) {
      case 'join_room':
        try {
          const { roomName, password, nickname } = parsedMessage.payload;
          clientData.nickname = nickname || DEFAULT_NICKNAME;
          clientData.currentRoom = roomName;

          const topicBuffer = generateTopicBuffer(roomName, password || '');
          const topicHex = topicBuffer.toString('hex'); // Get topicHex
          topicHexToRoomName.set(topicHex, roomName); // Store mapping

          await p2pNode.joinTopic(topicBuffer, clientData.nickname);

          await db.getOrCreateRoom({
            room_name: roomName,
            has_password: !!password,
            creator_node_id: clientId,
          });
          const historyMessages = await db.getRecentMessages(roomName, 50);
          const onlineUsersList = p2pNode
            .getPeersInRoom(topicBuffer)
            .map((peer) => ({ id: peer.nodeId, nickname: peer.nickname }));
          // Add self to online users list for the client
          onlineUsersList.unshift({ id: clientId, nickname: clientData.nickname });

          ws.send(
            JSON.stringify({
              type: 'room_joined',
              payload: {
                roomName: roomName,
                nickname: clientData.nickname,
                messages: historyMessages,
                onlineUsers: onlineUsersList,
              },
            })
          );
          console.log(`Client ${clientData.nickname} joined room: ${roomName}`);
        } catch (error) {
          console.error('Error joining room:', error);
          ws.send(
            JSON.stringify({
              type: 'error',
              payload: { message: `Failed to join room: ${error.message}` },
            })
          );
        }
        break;

      case 'client_chat_message':
        try {
          if (!clientData.currentRoom) {
            ws.send(
              JSON.stringify({ type: 'error', payload: { message: 'Not joined to any room.' } })
            );
            return;
          }
          // Check for AI command
          if (parsedMessage.content.startsWith('/ai ')) {
            const query = parsedMessage.content.substring(4);
            const aiResponse = await callGeminiService(query);
            ws.send(
              JSON.stringify({
                type: 'personal_ai_response',
                payload: { query, response: aiResponse },
              })
            );
          } else {
            const messageData = {
              sender_id: clientId,
              sender_nickname: clientData.nickname,
              message_content: parsedMessage.content,
              room_name: clientData.currentRoom,
              message_type: 'user_message',
              timestamp: new Date().toISOString(),
            };
            const savedMessage = await db.saveMessage(messageData);

            // 1. Broadcast to P2P network (for other peers)
            p2pNode.broadcast(savedMessage);

            // 2. Send message directly back to the originating client via its WebSocket
            // Ensure client is still in the same room as the message
            if (ws && clientData.currentRoom === savedMessage.room_name) {
              console.log(
                `[Self-Echo] Sending message directly to originating client ${clientData.id} in room ${clientData.currentRoom}`
              );
              ws.send(
                JSON.stringify({
                  type: 'chat_message', // Frontend expects this type
                  payload: savedMessage,
                })
              );
            } else {
              console.warn(
                `[Self-Echo] Conditions not met to send message back to client ${clientData.id}. Client room: ${clientData.currentRoom}, Message room: ${savedMessage.room_name}`
              );
            }
          }
        } catch (error) {
          console.error('Error processing client chat message:', error);
          ws.send(
            JSON.stringify({ type: 'error', payload: { message: 'Failed to process message.' } })
          );
        }
        break;

      case 'leave_room':
        if (clientData.currentRoom) {
          const topicBuffer = generateTopicBuffer(clientData.currentRoom, '');
          p2pNode.leaveTopic(topicBuffer, clientData.nickname);
          console.log(`Client ${clientData.nickname} left room: ${clientData.currentRoom}`);
          clientData.currentRoom = null;
          ws.send(
            JSON.stringify({ type: 'room_left', payload: { roomName: clientData.currentRoom } })
          );
        }
        break;

      default:
        console.warn(`Unknown message type: ${parsedMessage.type}`);
        ws.send(
          JSON.stringify({
            type: 'error',
            payload: { message: `Unknown message type: ${parsedMessage.type}` },
          })
        );
    }
  });

  ws.on('close', () => {
    const clientData = clients.get(ws);
    if (clientData && clientData.currentRoom) {
      const topicBuffer = generateTopicBuffer(clientData.currentRoom, '');
      p2pNode.leaveTopic(topicBuffer, clientData.nickname); // Inform peers about leaving
      console.log(`Client ${clientData.nickname} disconnected from room ${clientData.currentRoom}`);
    }
    clients.delete(ws);
    console.log('Client disconnected');
  });

  ws.on('error', (error) => {
    console.error('WebSocket error:', error);
    const clientData = clients.get(ws);
    if (clientData) {
      console.error(`Error for client: ${clientData.nickname}`);
    }
  });
});

// P2P Event Handlers
p2pNode.on('message', (messageData) => {
  console.log('[P2P Event - message] Received P2P message:', JSON.stringify(messageData, null, 2)); // DEBUG LOG
  clients.forEach((clientInfo, clientWs) => {
    console.log(
      `[P2P Event - message] Checking client: ${clientInfo.id}, room: ${clientInfo.currentRoom}. Message room: ${messageData.room_name}`
    ); // DEBUG LOG
    if (clientInfo.currentRoom === messageData.room_name) {
      console.log(
        `[P2P Event - message] Room match for client ${clientInfo.id}. Sending to WebSocket.`
      ); // DEBUG LOG
      // Avoid echo if sender_id is this node's own ID.
      // App.vue might also have its own duplicate prevention.
      // if (messageData.sender_id !== clientInfo.id) { // MODIFIED: Commenting out this line to allow self-echo
      clientWs.send(
        JSON.stringify({
          type: 'chat_message',
          payload: messageData,
        })
      );
      // }
    } else {
      console.log(
        `[P2P Event - message] Room mismatch for client ${clientInfo.id}: client room '${clientInfo.currentRoom}' vs message room '${messageData.room_name}'.`
      ); // DEBUG LOG
    }
  });
});

p2pNode.on('peer_joined_room', ({ roomName, peerInfo }) => {
  // peerInfo: { nodeId, nickname }
  clients.forEach((clientInfo, clientWs) => {
    if (clientInfo.currentRoom === roomName) {
      clientWs.send(
        JSON.stringify({
          type: 'presence_update', // Or 'user_joined'
          payload: { roomName, user: peerInfo, event: 'joined' },
        })
      );
    }
  });
});

p2pNode.on('peer_left_room', ({ roomName, peerInfo }) => {
  // peerInfo: { nodeId, nickname }
  clients.forEach((clientInfo, clientWs) => {
    if (clientInfo.currentRoom === roomName) {
      clientWs.send(
        JSON.stringify({
          type: 'presence_update', // Or 'user_left'
          payload: { roomName, user: peerInfo, event: 'left' },
        })
      );
    }
  });
});

// Optional: Handle nickname updates if your P2PNode emits 'peer_updated_room'
// p2pNode.on('peer_updated_room', ({ topicHex, nodeId: peerNodeId, nickname: newNickname, oldNickname }) => { ... });

async function startServer() {
  try {
    await db.connect();
    console.log('Database connected successfully.');
    await db.initializeSystemStats(); // Ensure system_stats table and records exist

    await p2pNode.initialize(); // MODIFIED: init -> initialize
    console.log(`P2P Node initialized with ID: ${p2pNode.getNodeId()}`); // MODIFIED: getPublicKeyHex -> getNodeId

    server.listen(PORT, HOST_IP, () => {
      // MODIFIED: Ensure HOST_IP is used if defined and applicable
      console.log(
        `User Node server listening on http://${
          HOST_IP === '0.0.0.0' ? 'localhost' : HOST_IP
        }:${PORT}`
      );
      console.log(`Announcing IP ${ANNOUNCED_IP} for P2P connections.`);
      if (BOOTSTRAP_SERVER_URL) {
        console.log(`Registering with bootstrap server: ${BOOTSTRAP_SERVER_URL}`);
        // MODIFIED: Direct call to bootstrapClient
        bootstrapClient
          .registerNode({
            id: p2pNode.getNodeId(),
            address: ANNOUNCED_IP,
            port: PORT,
            type: 'user',
          })
          .then(() =>
            console.log(`[Bootstrap] Successfully registered with ${BOOTSTRAP_SERVER_URL}`)
          )
          .catch((err) =>
            console.error(
              `[Bootstrap] Failed to register with ${BOOTSTRAP_SERVER_URL}:`,
              err.message
            )
          );
      } else {
        console.warn('BOOTSTRAP_SERVER_URL is not defined. P2P discovery might be limited.');
      }
    });
  } catch (err) {
    console.error('Failed to start server or connect to DB/P2P:', err);
    process.exit(1);
  }
}

// Graceful Shutdown
async function shutdown() {
  console.log('Shutting down server...');
  if (p2pNode) {
    if (BOOTSTRAP_SERVER_URL) {
      try {
        // MODIFIED: Direct call to bootstrapClient
        await bootstrapClient.unregisterNode();
        console.log(`[Bootstrap] Successfully unregistered from ${BOOTSTRAP_SERVER_URL}`);
      } catch (error) {
        console.error(
          `[Bootstrap] Failed to unregister from ${BOOTSTRAP_SERVER_URL}:`,
          error.message
        );
      }
    }
    await p2pNode.disconnect(); // MODIFIED: destroy -> disconnect
    console.log('P2P node shut down.');
  }
  await db.disconnect();
  console.log('Database disconnected.');
  wss.close(() => {
    server.close(() => {
      console.log('Server shut down gracefully.');
      process.exit(0);
    });
  });

  // Force shutdown if graceful close takes too long
  setTimeout(() => {
    console.error('Graceful shutdown timed out. Forcing exit.');
    process.exit(1);
  }, 5000);
}

process.on('SIGINT', shutdown);
process.on('SIGTERM', shutdown);

startServer();
