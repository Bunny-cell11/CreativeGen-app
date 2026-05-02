// complianceEngine.js
// Simple rule-check stub — extend by adding real rules and AI/OCR checks
module.exports = {
  runChecks: (creative) => {
    // creative.elements -> array of {type: 'image'|'text', x, y, width, height, text}
    const issues = [];

    // Example: basic overlap check (very simple)
    for (let i = 0; i < creative.elements.length; i++) {
      for (let j = i+1; j < creative.elements.length; j++) {
        const a = creative.elements[i], b = creative.elements[j];
        if (!a.width || !b.width) continue;
        const overlapX = Math.max(0, Math.min(a.x+a.width, b.x+b.width) - Math.max(a.x, b.x));
        const overlapY = Math.max(0, Math.min(a.y+a.height, b.y+b.height) - Math.max(a.y, b.y));
        if (overlapX > 0 && overlapY > 0) {
          issues.push({ type: 'overlap', elements: [i,j], message: 'Elements overlap' });
        }
      }
    }

    // Example: contrast check stub (always passes)
    // More checks: size limits, required elements, safe zone, banned words, etc.

    return { ok: issues.length === 0, issues };
  }
};

