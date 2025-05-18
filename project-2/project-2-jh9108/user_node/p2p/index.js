const Hyperswarm = require('hyperswarm');
// const crypto = require('crypto'); // crypto will be used in server.js to generate topic buffer
const { EventEmitter } = require('events');

// Removed hardcoded P2P_TOPIC_NAME and P2P_TOPIC

class P2PNode extends EventEmitter {
  constructor(nodeId) {
    super();
    this.swarm = new Hyperswarm();
    this.connections = new Set(); // Stores active P2P sockets for the current topic
    this.nodeId = nodeId || this.swarm.keyPair.publicKey.toString('hex');
    this.currentTopicBuffer = null; // To store the buffer of the current joined topic
    this.currentNicknamesByTopic = new Map(); // Map<topicHex, nickname> - Stores this node's nickname for each topic it's part of
    this.roomPeers = new Map(); // Map<topicHex, Map<peerNodeId, {nickname, swarmPeerId}>> - Stores peers in each room (topic)

    console.log(`[P2P] P2PNode instance created. Application Node ID: ${this.nodeId}`);
    this._initializeSwarmEvents();
  }

  _initializeSwarmEvents() {
    this.swarm.on('connection', (socket, peerInfo) => {
      const swarmPeerId = peerInfo.publicKey.toString('hex');
      const currentTopicHex = this.currentTopicBuffer
        ? this.currentTopicBuffer.toString('hex')
        : null;

      console.log(
        `[P2P] New connection from swarm peer: ${swarmPeerId} (Topic: ${
          currentTopicHex ? currentTopicHex.substring(0, 6) + '...' : 'N/A'
        })`
      );
      this.connections.add(socket);
      // Note: We don't know the peer's application-level nodeId or nickname yet.
      // We'll learn it when they send a presence_update message.
      this.emit('connection', socket, swarmPeerId);

      socket.on('data', (data) => {
        this._handleP2PData(socket, data, swarmPeerId, currentTopicHex);
      });

      socket.on('close', () => {
        console.log(
          `[P2P] Connection closed with swarm peer: ${swarmPeerId} (Topic: ${
            currentTopicHex ? currentTopicHex.substring(0, 6) + '...' : 'N/A'
          })`
        );
        this.connections.delete(socket);
        this.emit('disconnection', swarmPeerId);

        // Handle peer departure from roomPeers
        if (currentTopicHex && this.roomPeers.has(currentTopicHex)) {
          const peersInRoom = this.roomPeers.get(currentTopicHex);
          let departedPeerNodeId = null;
          for (const [peerNodeId, peerData] of peersInRoom.entries()) {
            if (peerData.swarmPeerId === swarmPeerId) {
              departedPeerNodeId = peerNodeId;
              break;
            }
          }

          if (departedPeerNodeId) {
            const departedPeerInfo = peersInRoom.get(departedPeerNodeId);
            peersInRoom.delete(departedPeerNodeId);
            console.log(
              `[P2P] Peer ${departedPeerNodeId} (nickname: ${departedPeerInfo.nickname}) removed from room ${currentTopicHex} due to P2P connection close.`
            );
            this.emit('peer_left_room', {
              topicHex: currentTopicHex,
              nodeId: departedPeerNodeId,
              nickname: departedPeerInfo.nickname,
            });
          } else {
            console.log(
              `[P2P] A swarm peer ${swarmPeerId} disconnected, but was not found in the roomPeers list for topic ${currentTopicHex}. May not have announced presence yet.`
            );
          }
        }
      });

      socket.on('error', (error) => {
        console.error(
          `[P2P] Connection error with swarm peer ${swarmPeerId} (Topic: ${
            currentTopicHex ? currentTopicHex.substring(0, 6) + '...' : 'N/A'
          }):`,
          error
        );
        this.connections.delete(socket);
        // We could also trigger peer_left_room here if the peer was known, similar to 'close' event.
        // For simplicity, 'close' event will handle the cleanup.
        this.emit('connection_error', { error, swarmPeerId });
      });
    });
  }

  _handleP2PData(socket, data, swarmPeerId, topicHexForThisConnection) {
    try {
      const message = JSON.parse(data.toString());
      const messageTopicHex = message.topicHex || topicHexForThisConnection;

      if (!messageTopicHex) {
        console.warn(
          `[P2P] Received message from ${swarmPeerId} without topicHex and connection has no current topic. Ignoring.`
        );
        return;
      }

      // Ensure currentTopicBuffer is set and matches the message's context if strictly enforcing
      // For now, we use messageTopicHex primarily for roomPeers map keying.

      switch (message.type) {
        case 'presence_update':
          if (!message.sender_id || !message.nickname) {
            console.warn(
              '[P2P] Invalid presence_update message, missing sender_id or nickname:',
              message
            );
            return;
          }
          if (!this.roomPeers.has(messageTopicHex)) {
            this.roomPeers.set(messageTopicHex, new Map());
          }
          const peersInRoom = this.roomPeers.get(messageTopicHex);
          // If peer is already known, update their info (e.g. nickname change, though not supported yet)
          // Or if their swarmPeerId changed (e.g. reconnect), update that too.
          const existingPeer = peersInRoom.get(message.sender_id);
          peersInRoom.set(message.sender_id, {
            nickname: message.nickname,
            swarmPeerId: swarmPeerId, // Associate app-level nodeId with P2P swarmPeerId
          });
          console.log(
            `[P2P] Presence update: Peer ${message.sender_id} (nickname: ${message.nickname}) in room ${messageTopicHex}. Total peers in room: ${peersInRoom.size}`
          );
          // Emit peer_joined_room only if it's a new peer for this room or a significant update
          if (!existingPeer) {
            this.emit('peer_joined_room', {
              topicHex: messageTopicHex,
              nodeId: message.sender_id,
              nickname: message.nickname,
            });
          } else if (existingPeer.nickname !== message.nickname) {
            // If nickname changed, emit an update (or treat as join for simplicity for now)
            this.emit('peer_updated_room', {
              topicHex: messageTopicHex,
              nodeId: message.sender_id,
              nickname: message.nickname,
              oldNickname: existingPeer.nickname,
            });
          }
          break;
        case 'presence_leave':
          if (!message.sender_id) {
            console.warn('[P2P] Invalid presence_leave message, missing sender_id:', message);
            return;
          }
          if (this.roomPeers.has(messageTopicHex)) {
            const peersInLeaveRoom = this.roomPeers.get(messageTopicHex);
            if (peersInLeaveRoom.has(message.sender_id)) {
              const departedPeer = peersInLeaveRoom.get(message.sender_id);
              peersInLeaveRoom.delete(message.sender_id);
              console.log(
                `[P2P] Presence leave: Peer ${message.sender_id} (nickname: ${departedPeer.nickname}) left room ${messageTopicHex}. Remaining peers: ${peersInLeaveRoom.size}`
              );
              this.emit('peer_left_room', {
                topicHex: messageTopicHex,
                nodeId: message.sender_id,
                nickname: departedPeer.nickname,
              });
            } else {
              console.log(
                `[P2P] Received presence_leave for ${message.sender_id} in ${messageTopicHex}, but peer was not in roomPeers list.`
              );
            }
          } else {
            console.log(
              `[P2P] Received presence_leave for ${message.sender_id} in ${messageTopicHex}, but room was not in roomPeers map.`
            );
          }
          break;
        default:
          // This is a regular chat message or other custom message type
          if (!message.sender_id) {
            console.warn(`[P2P] Received message from ${swarmPeerId} without app-level sender_id.`);
            // Optionally, inject swarmPeerId if sender_id is missing and it's a non-presence message
          }
          this.emit('message', {
            ...message,
            _swarm_peer_id: swarmPeerId,
            _topic_hex_internal: messageTopicHex,
          });
          break;
      }
    } catch (error) {
      console.error(
        '[P2P] Error parsing message from peer or in _handleP2PData:',
        error,
        data.toString()
      );
      this.emit('message_parse_error', { rawData: data.toString(), error, swarmPeerId });
    }
  }

  // Initialize the swarm itself, but don't join a topic yet.
  async initialize() {
    console.log(`[P2P] Initializing P2P communication system for Node ID: ${this.nodeId}`);
    // Potentially discover public key or other swarm setup if needed before joining a topic
    // For now, Hyperswarm handles most of this automatically.
    return Promise.resolve();
  }

  async joinTopic(topicBuffer, nickname) {
    if (!(topicBuffer instanceof Buffer)) {
      console.error('[P2P] Invalid topicBuffer provided. Must be a Buffer.');
      throw new Error('topicBuffer must be a Buffer');
    }
    if (typeof nickname !== 'string' || nickname.trim() === '') {
      console.error('[P2P] Invalid nickname provided for joinTopic. Must be a non-empty string.');
      throw new Error('Nickname must be a non-empty string');
    }
    const newTopicHex = topicBuffer.toString('hex');
    console.log(
      `[P2P] Node ${this.nodeId} (Nickname: ${nickname}) attempting to join topic: ${newTopicHex}`
    );

    if (this.currentTopicBuffer) {
      const oldTopicHex = this.currentTopicBuffer.toString('hex');
      console.log(`[P2P] Leaving current topic: ${oldTopicHex}`);
      const oldNickname =
        this.currentNicknamesByTopic.get(oldTopicHex) || this.nodeId.substring(0, 6); // Use a fallback for old nickname

      // Announce departure from the old topic to its connected peers
      const oldTopicConnections = Array.from(this.connections);
      const leaveMessage = JSON.stringify({
        type: 'presence_leave',
        sender_id: this.nodeId,
        nickname: oldNickname,
        topicHex: oldTopicHex,
      });
      console.log(
        `[P2P] Broadcasting presence_leave to ${oldTopicConnections.length} peers on old topic ${oldTopicHex}`
      );
      for (const socket of oldTopicConnections) {
        // Check if socket is still writable and belongs to the old topic context
        // This check is somewhat implicit as this.connections should only contain current topic connections after a join.
        // The broadcast method itself checks this.connections.size for the *current* topic.
        // This explicit broadcast is for connections on the *old* topic *before* they are cleared.
        try {
          socket.write(leaveMessage);
        } catch (e) {
          console.warn(`[P2P] Error writing leave message to old socket: ${e.message}`);
        }
      }

      this.currentNicknamesByTopic.delete(oldTopicHex);
      if (this.roomPeers.has(oldTopicHex)) {
        this.roomPeers.delete(oldTopicHex);
        console.log(`[P2P] Cleared peers map for old topic ${oldTopicHex}`);
      }

      try {
        await this.swarm.leave(this.currentTopicBuffer);
      } catch (leaveError) {
        console.warn(
          '[P2P] Error leaving current topic (continuing to join new topic):',
          leaveError.message
        );
      }
      // Destroy sockets associated with the old topic.
      // This is tricky because this.connections might already be for the new topic if join happens fast.
      // The oldTopicConnections set is more reliable here.
      oldTopicConnections.forEach((socket) => {
        if (!socket.destroyed) socket.destroy();
      });
      this.connections.clear(); // Clear all, new connections will be added for the new topic
    }

    this.currentTopicBuffer = topicBuffer;
    this.currentNicknamesByTopic.set(newTopicHex, nickname);
    if (!this.roomPeers.has(newTopicHex)) {
      // Initialize if not already (e.g. rejoining)
      this.roomPeers.set(newTopicHex, new Map());
    }

    try {
      await this.swarm.join(this.currentTopicBuffer, {
        server: true,
        client: true,
      });
      await this.swarm.flush();
      console.log(
        `[P2P] Successfully joined topic: ${newTopicHex} as ${nickname}. Listening for connections.`
      );

      // Connections for the new topic will be established via 'connection' event.
      // We broadcast presence_update to *newly established* connections for this topic.
      // However, it's better to broadcast to all current connections on this new topic
      // as some might have connected before this node fully flushed its join.
      this.broadcast({
        type: 'presence_update',
        sender_id: this.nodeId,
        nickname: nickname,
        topicHex: newTopicHex,
      });
    } catch (joinError) {
      console.error(`[P2P] Error joining topic ${newTopicHex}:`, joinError);
      this.currentNicknamesByTopic.delete(newTopicHex);
      if (this.roomPeers.has(newTopicHex)) {
        this.roomPeers.delete(newTopicHex);
      }
      this.currentTopicBuffer = null;
      throw joinError;
    }
  }

  getNodeId() {
    return this.nodeId;
  }

  getPeersInRoom(topicBuffer) {
    if (!topicBuffer || !(topicBuffer instanceof Buffer)) {
      console.warn('[P2P] getPeersInRoom called with invalid topicBuffer');
      return [];
    }
    const topicHex = topicBuffer.toString('hex');
    const peersMap = this.roomPeers.get(topicHex);
    if (peersMap) {
      // Convert Map values to an array of { nodeId, nickname }
      // The keys of peersMap are nodeId, and values are { nickname, swarmPeerId }
      return Array.from(peersMap.entries()).map(([nodeId, peerData]) => ({
        nodeId: nodeId,
        nickname: peerData.nickname,
      }));
    }
    return []; // Return empty array if room or peers not found
  }

  getActiveTopics() {
    return Array.from(this.roomPeers.keys()); // Returns an array of topicHex strings
  }

  broadcast(message) {
    if (!this.currentTopicBuffer) {
      console.warn('[P2P] Cannot broadcast: Not currently joined to any topic.');
      return;
    }
    // This broadcast goes to peers connected on the *currentTopicBuffer*
    if (this.connections.size === 0) {
      console.log(
        `[P2P] Broadcasting message to 0 peers (no active connections on current topic ${this.currentTopicBuffer.toString(
          'hex',
          0,
          6
        )}...).`,
        message
      );
      return;
    }

    let messageString;
    try {
      messageString = JSON.stringify(message);
    } catch (e) {
      console.error('[P2P] Failed to stringify message for broadcast:', e);
      return;
    }

    console.log(
      `[P2P] Broadcasting to ${
        this.connections.size
      } peers on topic ${this.currentTopicBuffer.toString(
        'hex',
        0,
        6
      )}...: ${messageString.substring(0, 100)}...`
    );
    for (const socket of this.connections) {
      try {
        socket.write(messageString);
      } catch (e) {
        console.warn(`[P2P] Error writing broadcast to socket: ${e.message}`);
      }
    }
  }

  async disconnect() {
    console.log('[P2P] Disconnecting from P2P network...');
    if (this.currentTopicBuffer) {
      const topicHex = this.currentTopicBuffer.toString('hex');
      const nickname = this.currentNicknamesByTopic.get(topicHex) || this.nodeId.substring(0, 6);
      console.log(`[P2P] Announcing presence_leave and leaving current topic: ${topicHex}`);
      this.broadcast({
        // Broadcast to current topic connections before leaving
        type: 'presence_leave',
        sender_id: this.nodeId,
        nickname: nickname,
        topicHex: topicHex,
      });
      try {
        await this.swarm.leave(this.currentTopicBuffer);
      } catch (leaveError) {
        console.warn('[P2P] Error leaving topic during disconnect:', leaveError.message);
      }
      this.currentNicknamesByTopic.delete(topicHex);
      if (this.roomPeers.has(topicHex)) {
        this.roomPeers.delete(topicHex);
      }
      this.currentTopicBuffer = null;
    }
    this.connections.forEach((socket) => {
      if (!socket.destroyed) socket.destroy();
    });
    this.connections.clear();
    if (this.swarm && !this.swarm.destroyed) {
      await this.swarm.destroy({ force: true }); // Added force: true for potentially faster/cleaner shutdown
      console.log('[P2P] Swarm destroyed.');
    } else {
      console.log('[P2P] Swarm already destroyed or not initialized.');
    }
    console.log('[P2P] Disconnected from P2P network.');
  }

  async topicLeft() {
    this.emit('topic_left', { topicHex: this.currentTopicBuffer.toString('hex') });
    this.currentTopicBuffer = null;
    console.log('[P2P] Left topic and destroyed connections.');
  }
}

module.exports = { P2PNode };
