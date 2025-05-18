import { GoogleGenAI } from '@google/genai';

const API_KEY = 'AIzaSyDfrAi2lVa1uY2hgZ8WdMKy6sc59pfkPxE';
const MODEL_NAME = 'gemini-2.0-flash';

async function runTest() {
  console.log('Starting Gemini API test...');
  try {
    const genAI = new GoogleGenAI({ apiKey: API_KEY });

    const testQuery = 'Explain what a Large Language Model is in simple terms.';

    const result = await genAI.models.generateContent({
      model: MODEL_NAME,
      contents: testQuery,
    });

    const outputText = result.text;

    console.log('\nGemini Response:');
    console.log(outputText);
  } catch (e: any) {
    console.error('Error during Gemini API test:', e);
    if (e && e.message && e.message.includes('API key not valid')) {
      console.error(
        'Please ensure your GEMINI_API_KEY is correct and has the necessary permissions.'
      );
    }
  }
  console.log('Gemini API test finished.');
}

runTest();
