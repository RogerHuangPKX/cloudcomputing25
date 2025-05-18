const axios = require('axios');

const BOOTSTRAP_SERVER_URL = 'http://20.121.46.47:3000'; // 更新为 Azure 服务器地址

let currentNodeInfo = null; // Stores the original nodeInfo { id, address, port, type }
let nodesList = [];
let refreshIntervalId = null;

/**
 * Registers the current node with the bootstrap server.
 * @param {Object} nodeInfo - Information about the current node.
 * @param {string} nodeInfo.id - Unique ID of the node.
 * @param {string} nodeInfo.address - IP address of the node.
 * @param {number} nodeInfo.port - Port the node is listening on.
 * @param {string} nodeInfo.type - Type of the node.
 */
async function registerNode(nodeInfo) {
  if (!nodeInfo || !nodeInfo.id || !nodeInfo.address || !nodeInfo.port || !nodeInfo.type) {
    console.error('[BootstrapClient] Invalid nodeInfo for registration:', nodeInfo);
    throw new Error('Invalid node information provided for registration.');
  }
  try {
    const payload = {
      nodeId: nodeInfo.id,
      ip: nodeInfo.address,
      port: nodeInfo.port,
      type: nodeInfo.type
    };
    console.log(`[BootstrapClient] Registering node with payload:`, payload);
    const response = await axios.post(`${BOOTSTRAP_SERVER_URL}/register`, payload);
    currentNodeInfo = { ...nodeInfo }; // Store the original structure
    console.log('[BootstrapClient] Node registered successfully:', response.data);
    await refreshNodesList(); // Initial fetch
    if (refreshIntervalId) {
      clearInterval(refreshIntervalId);
    }
    refreshIntervalId = setInterval(refreshNodesList, 30000); // Refresh every 30 seconds
    return response.data;
  } catch (error) {
    console.error('[BootstrapClient] Error registering node:', error.response ? error.response.data : error.message);
    throw error;
  }
}

/**
 * Fetches the list of active nodes from the bootstrap server.
 */
async function refreshNodesList() {
  if (!currentNodeInfo) {
    return;
  }
  try {
    const response = await axios.get(`${BOOTSTRAP_SERVER_URL}/nodes`);
    // Filter out the current node from the list based on its original ID
    nodesList = response.data.filter(node => node.nodeId !== currentNodeInfo.id);
    return nodesList;
  } catch (error) {
    console.error('[BootstrapClient] Error refreshing nodes list:', error.response ? error.response.data : error.message);
    return [];
  }
}

/**
 * Unregisters the current node from the bootstrap server.
 */
async function unregisterNode() {
  if (!currentNodeInfo || !currentNodeInfo.id) {
    console.warn('[BootstrapClient] No node information available to unregister.');
    return;
  }
  try {
    console.log(`[BootstrapClient] Unregistering node: ${currentNodeInfo.id}`);
    // Send nodeId to the server as expected by it
    await axios.post(`${BOOTSTRAP_SERVER_URL}/unregister`, { nodeId: currentNodeInfo.id });
    console.log('[BootstrapClient] Node unregistered successfully.');
    if (refreshIntervalId) {
      clearInterval(refreshIntervalId);
      refreshIntervalId = null;
    }
    currentNodeInfo = null;
    nodesList = [];
  } catch (error) {
    console.error('[BootstrapClient] Error unregistering node:', error.response ? error.response.data : error.message);
  }
}

/**
 * Returns the current list of other active nodes.
 * @returns {Array} List of node objects.
 */
function getNodeList() {
  return nodesList;
}

/**
 * Returns the information of the current registered node.
 * @returns {Object|null} Current node information or null if not registered.
 */
function getCurrentNodeInfo() {
  return currentNodeInfo;
}

module.exports = {
  registerNode,
  unregisterNode,
  getNodeList,
  getCurrentNodeInfo,
  refreshNodesList, // Exporting for potential manual refresh
};
