const axios = require("axios");
const fs = require("fs");
const path = require("path");
const FormData = require("form-data");

// OPENAI CONFIG
const OPENAI_KEY = process.env.OPENAI_API_KEY;

//
// 1) IMAGE GENERATION
//
exports.generateImage = async (req, res) => {
  try {
    const { prompt } = req.body;

    const result = await axios.post(
      "https://api.openai.com/v1/images/generations",
      {
        model: "gpt-image-1",
        prompt,
        size: "1024x1024",
      },
      {
        headers: {
          Authorization: `Bearer ${OPENAI_KEY}`,
          "Content-Type": "application/json",
        },
      }
    );

    res.json({
      image: result.data.data[0].url,
    });
  } catch (err) {
    console.log(err.response?.data);
    res.status(500).json({ error: "OpenAI image generation failed" });
  }
};

//
// 2) IMAGE EDIT / INPAINT
//
exports.editImage = async (req, res) => {
  try {
    const { prompt } = req.body;
    const file = req.file;

    const form = new FormData();
    form.append("prompt", prompt);
    form.append("image", fs.createReadStream(file.path));

    const result = await axios.post(
      "https://api.openai.com/v1/images/edits",
      form,
      {
        headers: {
          Authorization: `Bearer ${OPENAI_KEY}`,
          ...form.getHeaders(),
        },
      }
    );

    res.json({ image: result.data.data[0].url });
  } catch (err) {
    console.log(err.response?.data);
    res.status(500).json({ error: "OpenAI edit failed" });
  }
};

//
// 3) BACKGROUND REMOVAL USING OPENAI VISION
//
exports.removeBackgroundAI = async (req, res) => {
  try {
    const file = req.file;

    const form = new FormData();
    form.append("image", fs.createReadStream(file.path));

    const result = await axios.post(
      "https://api.openai.com/v1/images/edits",
      form,
      {
        headers: {
          Authorization: `Bearer ${OPENAI_KEY}`,
          ...form.getHeaders(),
        },
      }
    );

    res.json({ image: result.data.data[0].url });
  } catch (err) {
    console.log(err.response?.data);
    res.status(500).json({ error: "OpenAI background removal failed" });
  }
};

//
// 4) AI LAYOUT SUGGESTIONS (GPT-4.1 ANALYSIS)
//
exports.layoutSuggestions = async (req, res) => {
  try {
    const { elements } = req.body;

    const aiPrompt = `
You are a creative layout engine. Improve the layout:
${JSON.stringify(elements)}
Return JSON with updated x,y,width,height.
    `;

    const result = await axios.post(
      "https://api.openai.com/v1/chat/completions",
      {
        model: "gpt-4.1",
        messages: [{ role: "system", content: aiPrompt }],
      },
      {
        headers: {
          Authorization: `Bearer ${OPENAI_KEY}`,
          "Content-Type": "application/json",
        },
      }
    );

    const suggestion = JSON.parse(result.data.choices[0].message.content);

    res.json({ suggestions: suggestion });
  } catch (err) {
    console.log(err);
    res.status(500).json({ error: "OpenAI layout failed" });
  }
};

//
// 5) AI COMPLIANCE CHECKER
//
exports.complianceCheckAI = async (req, res) => {
  try {
    const { elements } = req.body;

    const result = await axios.post(
      "https://api.openai.com/v1/chat/completions",
      {
        model: "gpt-4.1",
        messages: [
          {
            role: "system",
            content:
              "You are a strict retail media compliance checker. Identify violations.",
          },
          {
            role: "user",
            content:
              "Check this creative for text readability, overlaps, safe zones, and brand rule violations:\n" +
              JSON.stringify(elements),
          },
        ],
      },
      {
        headers: {
          Authorization: `Bearer ${OPENAI_KEY}`,
          "Content-Type": "application/json",
        },
      }
    );

    res.json({
      aiFeedback: result.data.choices[0].message.content,
    });
  } catch (err) {
    console.log(err);
    res.status(500).json({ error: "AI compliance failed" });
  }
};

