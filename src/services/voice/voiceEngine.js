export default class VoiceEngine {
  constructor(onResult, onStart, onEnd) {
    const SpeechRecognition =
      window.SpeechRecognition || window.webkitSpeechRecognition;

    if (!SpeechRecognition) {
      alert("Voice recognition not supported in this browser.");
      return;
    }

    this.recognizer = new SpeechRecognition();
    this.recognizer.lang = "en-US";
    this.recognizer.interimResults = false;
    this.recognizer.continuous = false;

    this.recognizer.onstart = onStart;
    this.recognizer.onend = onEnd;

    this.recognizer.onresult = (event) => {
      const transcript = event.results[0][0].transcript.toLowerCase();
      onResult(transcript);
    };
  }

  start() {
    this.recognizer.start();
  }
}

