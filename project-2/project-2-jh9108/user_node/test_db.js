const db = require('./db');

async function runTest() {
  try {
    console.log('[TestDB] Connecting to database...');
    await db.connect();
    console.log('[TestDB] Connected.');

    const testMessage = {
      sender_id: 'test-user-001',
      sender_nickname: 'Tester',
      message_content: 'Hello from test_db.js! Timestamp: ' + new Date().toISOString(),
      message_type: 'test_message'
    };

    console.log('\n[TestDB] Saving test message:', testMessage);
    const savedMessage = await db.saveMessage(testMessage);
    console.log('[TestDB] Message saved:', savedMessage);

    console.log('\n[TestDB] Getting recent messages (limit 5)...');
    const recentMessages = await db.getRecentMessages(5);
    console.log('[TestDB] Recent messages fetched:');
    recentMessages.forEach(msg => console.log(msg));

    // Test fetching more messages
    console.log('\n[TestDB] Getting recent messages (limit 2)...');
    const fewerMessages = await db.getRecentMessages(2);
    console.log('[TestDB] Recent messages fetched (limit 2):');
    fewerMessages.forEach(msg => console.log(msg));


  } catch (error) {
    console.error('[TestDB] Error during database test:', error);
  } finally {
    console.log('\n[TestDB] Disconnecting from database...');
    await db.disconnect();
    console.log('[TestDB] Disconnected.');
  }
}

runTest();
