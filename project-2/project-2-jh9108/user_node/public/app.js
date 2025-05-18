const { createApp, ref, reactive, computed, nextTick, onMounted } = Vue;

const app = createApp({
  setup() {
    const nodeId = ref(null);
    const messages = reactive([]);
    const newMessage = ref('');
    const connectionStatus = ref('connecting'); // connecting, connected, disconnected, error
    const errorMessage = ref(null); // Renamed from 'error' for clarity with general errors
    let errorTimeout = null; // For auto-dismissing errors
    const ws = ref(null);
    const messagesContainer = ref(null); // Renamed from messageArea for consistency with HTML

    // Room related state
    const roomNameInput = ref('');
    const roomPasswordInput = ref('');
    const nicknameInput = ref(localStorage.getItem('chat_nickname') || ''); // Load nickname from local storage
    const currentRoom = ref(null);
    const roomJoined = ref(false);
    const isLoadingRoom = ref(false);
    const publicRooms = reactive([]);
    const isLoadingRoomList = ref(false);
    const publicRoomsFetched = ref(false); // To know if we tried fetching rooms
    const isSidebarOpen = ref(false); // For responsive sidebar control
    const onlineUsers = ref([]); // Added for online users list

    const connectWebSocket = () => {
      const wsProtocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      // Use current hostname and explicitly defined backend port (e.g. 4000) if needed
      // For simplicity, assuming ws server is on same host and port as http for now,
      // but this might need adjustment if http is on 80/443 and ws on a different one (e.g. 4000)
      const wsHost = window.location.hostname;
      const wsPort = window.location.port || (window.location.protocol === 'https:' ? 443 : 80);
      // If your backend Express server (hosting WS) is on a different port than the static file server,
      // you might need to hardcode it or use an environment variable for the frontend.
      // For this example, we assume they are on the same port or handled by a proxy.
      const wsUrl = `${wsProtocol}//${wsHost}:${wsPort}`;

      connectionStatus.value = 'connecting';
      // errorMessage.value = null; // Replaced by clearError or setError(null)
      clearError(); // Clear any previous error on new connection attempt
      console.log(`Attempting to connect WebSocket to: ${wsUrl}`);

      ws.value = new WebSocket(wsUrl);

      ws.value.onopen = () => {
        console.log('WebSocket connection established');
        connectionStatus.value = 'connected';
        // No initial system message here, wait for initial_node_info or room join
      };

      ws.value.onmessage = (event) => {
        try {
          const receivedData = JSON.parse(event.data);
          console.log('Received data from server:', receivedData);

          if (receivedData.type === 'initial_node_info') {
            nodeId.value = receivedData.nodeId;
            messages.push({
              temp_id: Date.now(),
              message_content: `Connected to server. Your Node ID is ${nodeId.value}. Please join or create a room.`,
              message_type: 'system',
              timestamp: new Date().toISOString(),
            });
          } else if (receivedData.type === 'room_joined') {
            currentRoom.value = receivedData.payload.roomName;
            messages.length = 0; // Clear previous messages
            if (receivedData.payload.messages) {
              messages.push(...receivedData.payload.messages);
            }
            roomJoined.value = true;
            isLoadingRoom.value = false;
            clearError();

            // Update nickname if server assigned/confirmed one
            if (receivedData.payload.nickname) {
              nicknameInput.value = receivedData.payload.nickname;
              localStorage.setItem('chat_nickname', nicknameInput.value);
              console.log(`[App] Nickname confirmed/updated by server to: ${nicknameInput.value}`);
            }

            // Initialize online users list
            if (receivedData.payload.onlineUsers) {
              onlineUsers.value = receivedData.payload.onlineUsers;
            } else {
              onlineUsers.value = []; // Initialize as empty if not provided
            }

            messages.push({
              temp_id: Date.now() + 1,
              message_content: `Successfully joined room: "${currentRoom.value}". ${receivedData.payload.messages ? receivedData.payload.messages.length : 0} historical messages loaded.`,
              message_type: 'system',
              timestamp: new Date().toISOString(),
            });
          } else if (receivedData.type === 'user_joined_room_p2p') {
            const { nodeId: joinedNodeId, nickname: joinedNickname } = receivedData.payload;
            // Add to onlineUsers if not already present
            if (!onlineUsers.value.some((user) => user.nodeId === joinedNodeId)) {
              onlineUsers.value.push({ nodeId: joinedNodeId, nickname: joinedNickname });
            }
          } else if (
            receivedData.type === 'user_left_room_p2p' ||
            receivedData.type === 'user_left_web_client'
          ) {
            const { nodeId: leftNodeId } = receivedData.payload;
            onlineUsers.value = onlineUsers.value.filter((user) => user.nodeId !== leftNodeId);
          } else if (receivedData.type === 'personal_ai_response') {
            const { query, response, timestamp } = receivedData.payload;
            messages.push({
              temp_id: `ai_${Date.now()}`,
              message_type: 'local_ai_interaction', // For special rendering
              sender_nickname: 'My AI Assistant', // Or derive from user's nickname
              original_query: query,
              message_content: response, // Storing AI's response here for renderMarkdown
              timestamp: timestamp,
              // No sender_id needed as it's a local, private display
            });
            scrollToBottom(); // Scroll to show the AI response
          } else if (receivedData.type === 'chat_message') {
            messages.push(receivedData.payload);
          } else if (receivedData.type === 'error') {
            // errorMessage.value = `Server error: ${receivedData.message}`;
            setError(`Server error: ${receivedData.message}`);
            isLoadingRoom.value = false; // Stop loading if an error occurred during room join
            messages.push({
              temp_id: Date.now(),
              message_content: `Server error: ${receivedData.message}`,
              message_type: 'system',
              timestamp: new Date().toISOString(),
            });
          }
          scrollToBottom();
        } catch (e) {
          console.error('Error processing message from server:', e);
          // errorMessage.value = 'Error processing message from server.';
          setError('Error processing message from server.');
          isLoadingRoom.value = false;
        }
      };

      ws.value.onerror = (err) => {
        console.error('WebSocket error:', err);
        // errorMessage.value = 'WebSocket connection error. Please check the server or try refreshing.';
        setError('WebSocket connection error. Please check the server or try refreshing.');
        connectionStatus.value = 'error';
        isLoadingRoom.value = false;
        messages.push({
          temp_id: Date.now(),
          message_content: 'WebSocket connection error.',
          message_type: 'system',
          timestamp: new Date().toISOString(),
        });
      };

      ws.value.onclose = () => {
        console.log('WebSocket connection closed');
        connectionStatus.value = 'disconnected';
        roomJoined.value = false; // Reset room status on disconnect
        currentRoom.value = null;
        isLoadingRoom.value = false;
        messages.push({
          temp_id: Date.now(),
          message_content: 'Disconnected from chat server. Attempting to reconnect...',
          message_type: 'system',
          timestamp: new Date().toISOString(),
        });
        setTimeout(connectWebSocket, 5000);
      };
    };

    const handleJoinRoom = () => {
      if (!nicknameInput.value.trim() || !roomNameInput.value.trim()) {
        // errorMessage.value = 'Nickname and Room Name are required.';
        setError('Nickname and Room Name are required.');
        return;
      }
      if (ws.value && ws.value.readyState === WebSocket.OPEN) {
        isLoadingRoom.value = true;
        // errorMessage.value = null;
        clearError(); // Clear previous errors before attempting to join
        // Save nickname to local storage for persistence
        localStorage.setItem('chat_nickname', nicknameInput.value.trim());

        const payload = {
          roomName: roomNameInput.value.trim(),
          password: roomPasswordInput.value, // Send empty string if no password
          nickname: nicknameInput.value.trim(),
          // description: '' // Optional: add if you have a field for room description during creation
        };
        ws.value.send(JSON.stringify({ type: 'join_room', payload }));
        console.log('Sent join_room request:', payload);
      } else {
        // errorMessage.value = 'WebSocket is not connected. Please wait.';
        setError('WebSocket is not connected. Please wait.');
      }
    };

    const fetchPublicRooms = async () => {
      isLoadingRoomList.value = true;
      publicRoomsFetched.value = true;
      // errorMessage.value = null;
      clearError();
      try {
        const response = await fetch('/api/rooms/list');
        if (!response.ok) {
          const errorData = await response.json();
          throw new Error(errorData.message || `Failed to fetch public rooms: ${response.status}`);
        }
        const roomsData = await response.json();
        publicRooms.splice(0, publicRooms.length, ...roomsData);
      } catch (err) {
        console.error('Error fetching public rooms:', err);
        // errorMessage.value = err.message;
        setError(err.message);
        publicRooms.length = 0;
      }
      isLoadingRoomList.value = false;
    };

    const selectPublicRoomAndJoin = (selectedRoomName) => {
      roomNameInput.value = selectedRoomName;
      roomPasswordInput.value = ''; // Clear password when selecting a public room from the list

      if (nicknameInput.value.trim()) {
        if (currentRoom.value === selectedRoomName && roomJoined.value) {
          console.log(`[App] Already in room: ${selectedRoomName}`);
          // errorMessage.value = `You are already in room "${selectedRoomName}".`;
          // setTimeout(() => { if(errorMessage.value && errorMessage.value.includes(selectedRoomName)) errorMessage.value = null; }, 3000);
          setError(`You are already in room "${selectedRoomName}".`, 3000); // Use setError with custom timeout
          return;
        }
        handleJoinRoom();
      } else {
        // errorMessage.value = "Please enter your nickname before joining a room.";
        setError('Please enter your nickname before joining a room.');
        // Consider focusing the nickname input field
        // For example, if you add a ref="nicknameField" to the input:
        // nicknameField.value.focus();
      }
    };

    const sendMessage = () => {
      if (
        newMessage.value.trim() &&
        roomJoined.value &&
        ws.value &&
        ws.value.readyState === WebSocket.OPEN
      ) {
        const messagePayload = {
          type: 'client_chat_message',
          content: newMessage.value.trim(),
          // roomName is not needed here, server knows from ws.currentRoom
        };
        ws.value.send(JSON.stringify(messagePayload));
        newMessage.value = '';
        // scrollToBottom(); // Server echo will trigger scroll
      } else if (!roomJoined.value) {
        // errorMessage.value = 'You must join a room to send messages.';
        setError('You must join a room to send messages.');
      }
    };

    const scrollToBottom = () => {
      nextTick(() => {
        if (messagesContainer.value) {
          messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight;
        }
      });
    };

    // No longer using sortedMessages, as messages are pushed chronologically and history is prepended.
    // If complex sorting is ever needed again, it can be re-added.

    const formatTimestamp = (ts) => {
      if (!ts) return '';
      try {
        return new Date(ts).toLocaleTimeString([], {
          hour: '2-digit',
          minute: '2-digit',
          // second: '2-digit', // Optional: remove seconds for cleaner look
        });
      } catch (e) {
        return 'Invalid date';
      }
    };

    const renderMarkdown = (text) => {
      if (text && typeof marked === 'function') {
        return marked.parse(text, { breaks: true, gfm: true });
      } else if (text) {
        return text
          .replace(/&/g, '&amp;')
          .replace(/</g, '&lt;')
          .replace(/>/g, '&gt;')
          .replace(/\n/g, '<br>');
      }
      return '';
    };

    const clearError = () => {
      errorMessage.value = null;
      if (errorTimeout) {
        clearTimeout(errorTimeout);
        errorTimeout = null;
      }
    };

    const setError = (message, timeout = 7000) => {
      errorMessage.value = message;
      if (errorTimeout) {
        clearTimeout(errorTimeout);
      }
      if (message) {
        // Only set timeout if there's an actual error message
        errorTimeout = setTimeout(() => {
          clearError(); // Call clearError to ensure timeoutId is also cleared
        }, timeout);
      }
    };

    const handleLeaveRoom = () => {
      if (roomJoined.value) {
        const leftRoomName = currentRoom.value;
        roomJoined.value = false;
        currentRoom.value = null;
        messages.length = 0; // Clear messages
        onlineUsers.value = []; // Clear online users list
        // Optional: Clear room input fields
        // roomNameInput.value = '';
        // roomPasswordInput.value = '';

        messages.push({
          temp_id: Date.now(),
          message_content: `You have left room "${leftRoomName}". You can now join another room or create a new one.`,
          message_type: 'system',
          timestamp: new Date().toISOString(),
        });

        // We are not sending a message to the server or P2P node to leave the topic here.
        // The P2P topic will be switched upon next successful room join.
        // WebSocket connection remains active.
        console.log(`[App] Left room: ${leftRoomName}. UI reset.`);
        scrollToBottom(); // Scroll to show the system message
      }
    };

    const toggleSidebar = () => {
      isSidebarOpen.value = !isSidebarOpen.value;
      console.log('[App] Sidebar toggled:', isSidebarOpen.value);
    };

    // Initial connection
    onMounted(() => {
      connectWebSocket();
    });

    return {
      nodeId,
      messages, // Direct use, assuming server sends in order
      newMessage,
      connectionStatus,
      errorMessage, // Updated name
      clearError,
      setError, // Expose setError
      sendMessage,
      formatTimestamp,
      renderMarkdown,
      messagesContainer, // Updated name

      // Room related
      roomNameInput,
      roomPasswordInput,
      nicknameInput,
      currentRoom,
      roomJoined,
      isLoadingRoom,
      handleJoinRoom,
      publicRooms,
      isLoadingRoomList,
      fetchPublicRooms,
      selectPublicRoomAndJoin,
      publicRoomsFetched,
      handleLeaveRoom,
      isSidebarOpen, // Expose new ref
      toggleSidebar, // Expose new method
      onlineUsers, // Expose onlineUsers for the template
    };
  },
});

app.mount('#app');
