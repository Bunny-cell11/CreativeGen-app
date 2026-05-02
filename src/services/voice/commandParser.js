export function parseVoiceCommand(text) {
  text = text.toLowerCase();

  // -------- AI GENERATION --------
  if (text.includes("generate") || text.includes("create image"))
    return { action: "ai-generate", prompt: text.replace("generate", "").trim() };

  // -------- ADD TEXT --------
  if (text.startsWith("add text")) {
    const content = text.replace("add text", "").trim();
    return { action: "add-text", content };
  }

  // -------- BACKGROUND REMOVAL --------
  if (text.includes("remove background") || text.includes("remove bg"))
    return { action: "remove-bg" };

  // -------- UPSCALE --------
  if (text.includes("upscale"))
    return { action: "upscale" };

  // -------- ENHANCE --------
  if (text.includes("enhance"))
    return { action: "enhance" };

  // -------- CLEAN BACKGROUND --------
  if (text.includes("clean background"))
    return { action: "clean-bg" };

  // -------- FILL (Generative fill) --------
  if (text.includes("fill"))
    return { action: "fill" };

  // -------- AI LAYOUT --------
  if (text.includes("layout") || text.includes("arrange"))
    return { action: "ai-layout" };

  // -------- COMPLIANCE --------
  if (text.includes("check compliance") || text.includes("validate"))
    return { action: "ai-compliance" };

  // -------- SELECT ELEMENT --------
  if (text.includes("select") && text.includes("image"))
    return { action: "select-image" };

  return { action: "unknown", transcript: text };
}

