<template>
  <div class="flex flex-col h-full p-4 bg-gray-100">
    <div class="mb-4">
      <h1 class="text-3xl font-bold text-center text-blue-600">P2P Chat Room</h1>
      <div class="text-sm text-center text-gray-500">User ID: {{ nodeId || 'Connecting...' }}</div>
      <div class="text-sm text-center" :class="wsStatus === 'connected' ? 'text-green-500' : 'text-red-500'">
        WebSocket: {{ wsStatus }}
      </div>
    </div>

    <div class="flex-grow p-4 mb-4 overflow-y-auto bg-white border border-gray-300 rounded-lg shadow-md">
      <div v-for="(msg, index) in messages" :key="index" class="mb-3">
        <div
          :class="['p-3 rounded-lg max-w-xs lg:max-w-md xl:max-w-lg break-words', msg.isMine ? 'bg-blue-500 text-white ml-auto' : 'bg-gray-200 text-gray-800 mr-auto']"
        >
          <div class="text-xs font-semibold" v-if="!msg.isMine">{{ msg.sender_nickname || msg.sender_id.substring(0, 8) }}</div>
          <div>{{ msg.message_content }}</div>
          <div class="text-xs mt-1" :class="msg.isMine ? 'text-blue-200' : 'text-gray-500'">
            {{ new Date(msg.timestamp).toLocaleTimeString() }} <span v-if="msg.message_type === 'p2p'">(P2P)</span>
          </div>
        </div>
      </div>
    </div>

    <div class="flex p-2 bg-white border-t border-gray-300 rounded-b-lg shadow-md">
      <input
        type="text"
        v-model="newMessage"
        @keyup.enter="sendMessage"
        placeholder="Type your message..."
        class="flex-grow p-3 mr-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500"
        :disabled="wsStatus !== 'connected'"
      />
      <button
        @click="sendMessage"
        class="px-6 py-3 font-semibold text-white bg-blue-500 rounded-lg hover:bg-blue-600 focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-opacity-50 disabled:opacity-50"
        :disabled="wsStatus !== 'connected' || !newMessage.trim()"
      >
        Send
      </button>
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'https://unpkg.com/vue@3/dist/vue.esm-browser.js';

export default {
  name: 'App',
  setup() {
    const messages = ref([]);
    const newMessage = ref('');
    const nodeId = ref('');
    const ws = ref(null);
    const wsStatus = ref('disconnected'); // disconnected, connecting, connected, error
    const chatHistoryLoaded = ref(false);

    const scrollToBottom = () => {
      nextTick(() => {
        const container = document.querySelector(".overflow-y-auto");
        if (container) {
          container.scrollTop = container.scrollHeight;
        }
      });
    };

    const connectWebSocket = () => {
      wsStatus.value = 'connecting';
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      const wsUrl = `${protocol}//${window.location.host}`;

      console.log(`Attempting to connect to WebSocket at ${wsUrl}`);
      ws.value = new WebSocket(wsUrl);

      ws.value.onopen = () => {
        console.log('WebSocket connected');
        wsStatus.value = 'connected';
        // Request node ID or other initial info if needed
        // ws.value.send(JSON.stringify({ type: 'getNodeId' }));
        // Backend now sends initial data (nodeId and history) automatically on connection
      };

      ws.value.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data);
          console.log('Received message:', data);

          if (data.type === 'initial_data') {
            nodeId.value = data.nodeId;
            // console.log('Received initial node ID:', nodeId.value);
            const history = data.history || [];
            messages.value = history.map(msg => ({
              ...msg,
              isMine: msg.sender_id === nodeId.value,
            }));
            chatHistoryLoaded.value = true;
            // console.log('Chat history loaded:', messages.value);
            scrollToBottom();
          } else if (data.type === 'chat_message') {
            const incomingMessage = {
              ...data.payload,
              isMine: data.payload.sender_id === nodeId.value,
            };
            messages.value.push(incomingMessage);
            // console.log('New chat message:', incomingMessage);
            scrollToBottom();
          } else if (data.type === 'node_id_info') { // Kept for potential future use, but initial_data is preferred
            nodeId.value = data.nodeId;
            // console.log('Received node ID:', nodeId.value);
          } else if (data.type === 'error') {
            console.error('Server error:', data.message);
            alert(`Server error: ${data.message}`);
          }
        } catch (error) {
          console.error('Failed to parse message or handle incoming data:', error);
          // Handle non-JSON messages or parsing errors if necessary
          // For now, we assume all relevant messages are JSON
        }
      };

      ws.value.onclose = () => {
        console.log('WebSocket disconnected');
        wsStatus.value = 'disconnected';
        // Optionally, try to reconnect
        // setTimeout(connectWebSocket, 5000); // Reconnect after 5 seconds
      };

      ws.value.onerror = (error) => {
        console.error('WebSocket error:', error);
        wsStatus.value = 'error';
        // ws.value.close(); // Ensure connection is closed if not already
      };
    };

    const sendMessage = () => {
      if (newMessage.value.trim() && ws.value && ws.value.readyState === WebSocket.OPEN) {
        const messagePayload = {
          type: 'client_chat_message', // Differentiate from P2P messages from server
          content: newMessage.value.trim(),
          // sender_id will be added by the server based on WebSocket connection
        };
        ws.value.send(JSON.stringify(messagePayload));
        // console.log('Sent message:', messagePayload);
        // No need to add to messages array here, server will echo it back via P2P or direct WS message
        newMessage.value = '';
      } else {
        console.warn('Cannot send message. WebSocket not open or message is empty.');
      }
    };

    onMounted(() => {
      connectWebSocket();
    });

    onUnmounted(() => {
      if (ws.value) {
        ws.value.close();
      }
    });

    return {
      messages,
      newMessage,
      nodeId,
      wsStatus,
      sendMessage,
      // No need to expose connectWebSocket, called onMounted
    };
  }
};
</script>

<style scoped>
/* Scoped styles for App.vue can go here if needed */
/* Tailwind utility classes are preferred */
.max-w-xs { max-width: 20rem; /* 320px */ }
.lg\:max-w-md { max-width: 28rem; /* 448px */ }
.xl\:max-w-lg { max-width: 32rem; /* 512px */ }
</style>
