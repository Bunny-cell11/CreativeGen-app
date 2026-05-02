import React, { useState } from "react";
import KonvaEditor from "../components/KonvaEditor";

import api from "../services/api";
import { getToken } from "../services/auth";

export default function Editor() {
  const [name, setName] = useState("Untitled");

  // Save creative
  async function saveCreative(imageDataURL) {
    try {
      const token = getToken();

      const res = await api.post(
        "/creatives/create",
        { name, image: imageDataURL },
        {
          headers: { Authorization: `Bearer ${token}` },
        }
      );

      alert("Saved ✔");
    } catch (err) {
      console.error(err);
      alert("Save failed");
    }
  }

  // Check Compliance
  async function checkCompliance(elements) {
    try {
      const token = getToken();

      const res = await api.post("/creatives/check-compliance", elements, {
        headers: { Authorization: `Bearer ${token}` },
      });

      return res.data;
    } catch (err) {
      console.error(err);
      alert("Compliance check failed");
    }
  }

  return (
    <div style={{ padding: 20 }}>
      <h2>Editor</h2>

      <div style={{ marginBottom: 10 }}>
        <input
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="Creative Name"
        />
      </div>

      {/* 🔥 The correct component */}
      <KonvaEditor
        onSave={saveCreative}
        onCheckCompliance={checkCompliance}
      />
    </div>
  );
}
