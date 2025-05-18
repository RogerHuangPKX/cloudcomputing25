const express = require('express');
const app = express();
const PORT = process.env.PORT || 3000;

// Middleware to parse JSON bodies
app.use(express.json());

// In-memory store for online nodes
// node_id: { ip, port, type, last_seen }
let onlineNodes = {};

const NODE_TIMEOUT_MS = 60000; // 60 seconds, for example

// --- API Endpoints ---

// POST /register - Register a new node
app.post('/register', (req, res) => {
  const { nodeId, ip, port, type } = req.body;

  if (!nodeId || !ip || !port || !type) {
    return res.status(400).json({ message: 'Missing required fields: nodeId, ip, port, type' });
  }

  onlineNodes[nodeId] = {
    ip,
    port,
    type,
    last_seen: Date.now(),
  };

  console.log(`Node registered: ${nodeId} at ${ip}:${port} (${type})`);
  // Clean up stale nodes after registration as well
  cleanupStaleNodes();
  res
    .status(200)
    .json({ message: 'Node registered successfully', totalNodes: Object.keys(onlineNodes).length });
});

// GET /nodes - Get the list of online nodes
app.get('/nodes', (req, res) => {
  cleanupStaleNodes(); // Clean up before returning the list
  const activeNodes = [];
  for (const nodeId in onlineNodes) {
    activeNodes.push({
      nodeId: nodeId,
      ip: onlineNodes[nodeId].ip,
      port: onlineNodes[nodeId].port,
      type: onlineNodes[nodeId].type,
    });
  }
  res.status(200).json(activeNodes);
});

// POST /unregister - Unregister a node (optional, can also rely on timeout)
app.post('/unregister', (req, res) => {
  const { nodeId } = req.body;
  if (!nodeId) {
    return res.status(400).json({ message: 'Missing required field: nodeId' });
  }

  if (onlineNodes[nodeId]) {
    delete onlineNodes[nodeId];
    console.log(`Node unregistered: ${nodeId}`);
    res.status(200).json({ message: 'Node unregistered successfully' });
  } else {
    res.status(404).json({ message: 'Node not found' });
  }
});

// --- Helper Functions ---

// Function to remove stale nodes
function cleanupStaleNodes() {
  const now = Date.now();
  let cleanedCount = 0;
  for (const nodeId in onlineNodes) {
    if (now - onlineNodes[nodeId].last_seen > NODE_TIMEOUT_MS) {
      console.log(`Removing stale node: ${nodeId}`);
      delete onlineNodes[nodeId];
      cleanedCount++;
    }
  }
  if (cleanedCount > 0) {
    console.log(`Cleaned up ${cleanedCount} stale node(s).`);
  }
}

// Periodically clean up stale nodes (e.g., every 30 seconds)
setInterval(cleanupStaleNodes, NODE_TIMEOUT_MS / 2);

// Start the server
app.listen(PORT, () => {
  console.log(`Bootstrap server listening on port ${PORT}`);
  console.log('Registered endpoints:');
  console.log('  POST /register { nodeId, ip, port, type }');
  console.log('  GET  /nodes');
  console.log('  POST /unregister { nodeId }');
});

// Basic heartbeat mechanism: nodes can re-register to update last_seen
// Alternatively, a dedicated /heartbeat endpoint could be added if preferred.
// For now, re-registering (calling /register again with the same nodeId) will update the last_seen timestamp.

console.log('Bootstrap server script loaded. Starting...');
