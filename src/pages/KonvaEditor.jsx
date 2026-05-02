import React, { useState, useRef } from "react";

import {
  openaiGenerate,
  openaiEdit,
  openaiRemoveBg,
  openaiLayoutSuggestions,
  openaiCompliance,
  scalabilityUpscale,
  scalabilityEnhance,
  scalabilityRemoveBg,
  scalabilityFill,
  removeBgRoute,
} from "../services/ai/ai";

import VoiceEngine from "../services/voice/voiceEngine";
import { parseVoiceCommand } from "../services/voice/commandParser";

import api from "../services/api";
import { getToken } from "../services/auth";

import KonvaEditor from "../components/KonvaEditor";

export default function Editor() {
  const [name, setName] = useState("Untitled");
  const [preview, setPreview] = useState(null);
  const [listening, setListening] = useState(false);
  const [lastAction, setLastAction] = useState(null);

  const fileInputRef = useRef();

  const voice = useRef(
    new VoiceEngine(
      (speech) => handleVoiceCommand(speech),
      () => setListening(true),
      () => setListening(false)
    )
  ).current;

  const pickFile = () => fileInputRef.current.click();

  const aiGenerate = async () => {
    const prompt = prompt("Enter image prompt");
    if (!prompt) return;

    const imageUrl = await openaiGenerate(prompt);
    setElements((p) => [
      ...p,
      {
        id: crypto.randomUUID(),
        type: "image",
        url: imageUrl,
        x: 120,
        y: 120,
        width: 300,
        height: 300,
      },
    ]);
  };

  const handleFileSelect = async (file) => {
    let result;

    switch (lastAction) {
      case "edit":
        result = await openaiEdit(file, prompt("Edit prompt:"));
        break;
      case "remove-bg":
        result = await openaiRemoveBg(file);
        break;
      case "upscale":
        result = await scalabilityUpscale(file);
        break;
      case "enhance":
        result = await scalabilityEnhance(file);
        break;
      case "scalability-bg":
        result = await scalabilityRemoveBg(file);
        break;
      case "fill":
        result = await scalabilityFill(file);
        break;
      case "removebg-route":
        result = await removeBgRoute(file);
        break;
    }

    setPreview(result);
  };

  const aiLayout = async () => {
    const res = await openaiLayoutSuggestions(elements);
    setElements(res);
  };

  const aiComplianceCheck = async () => {
    const result = await openaiCompliance(elements);
    alert("AI Feedback:\n" + result);
  };

  const handleVoiceCommand = async (speech) => {
    const result = parseVoiceCommand(speech);

    switch (result.action) {
      case "ai-generate":
        await aiGenerateVoice(result.prompt);
        break;
      case "add-text":
        addTextVoice(result.content);
        break;
      case "ai-layout":
        await aiLayout();
        break;
      case "ai-compliance":
        await aiComplianceCheck();
        break;
      default:
        alert("Unknown command: " + speech);
    }
  };

  const aiGenerateVoice = async (prompt) => {
    const img = await openaiGenerate(prompt);
    setElements((p) => [
      ...p,
      {
        id: crypto.randomUUID(),
        type: "image",
        url: img,
        x: 100,
        y: 100,
        width: 300,
        height: 300,
      },
    ]);
  };

  const addTextVoice = (content) => {
    setElements((p) => [
      ...p,
      {
        id: crypto.randomUUID(),
        type: "text",
        text: content,
        x: 50,
        y: 50,
        fontSize: 26,
        width: 200,
        height: 40,
      },
    ]);
  };

  async function saveCreative(elements) {
    const token = getToken();
    await api.post(
      "/creatives/create",
      { name, elements: JSON.stringify(elements) },
      { headers: { Authorization: `Bearer ${token}` } }
    );
    alert("Saved");
  }

  async function checkCompliance(elements) {
    const token = getToken();
    const res = await api.post("/creatives/check-compliance", elements, {
      headers: { Authorization: `Bearer ${token}` },
    });
    return res.data;
  }

  return (
    <div style={{ padding: 20 }}>
      <h2>Editor</h2>

      <input value={name} onChange={(e) => setName(e.target.value)} />

      <button
        style={{
          background: listening ? "red" : "white",
          borderRadius: "50%",
          width: 40,
          height: 40,
        }}
        onClick={() => voice.start()}
      >
        🎤
      </button>

      {listening && <div>Listening...</div>}

      <input
        ref={fileInputRef}
        type="file"
        accept="image/*"
        style={{ display: "none" }}
        onChange={(e) => handleFileSelect(e.target.files[0])}
      />

      <KonvaEditor onSave={saveCreative} onCheckCompliance={checkCompliance} />
    </div>
  );
}

