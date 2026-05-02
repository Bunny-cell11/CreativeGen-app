import React, { useState, useRef } from "react";
import { Stage, Layer, Text, Image as KonvaImage } from "react-konva";
import { useImage } from "react-konva-utils";

function URLImage({ el, onSelect, onDragMove }) {
  const [img] = useImage(el.url);

  return (
    <KonvaImage
      image={img}
      x={el.x}
      y={el.y}
      width={el.width}
      height={el.height}
      draggable
      onClick={() => onSelect(el.id)}
      onTap={() => onSelect(el.id)}
      onDragMove={(e) =>
        onDragMove(el.id, {
          x: e.target.x(),
          y: e.target.y(),
        })
      }
    />
  );
}

export default function KonvaEditor({ onSave, onCheckCompliance }) {
  const [elements, setElements] = useState([]);
  const [guides, setGuides] = useState([]);

  const stageRef = useRef();
  const canvasSize = { width: 800, height: 1000 };

  // Add Text
  const addText = () => {
    setElements((p) => [
      ...p,
      {
        id: crypto.randomUUID(),
        type: "text",
        text: "New Text",
        x: 100,
        y: 100,
        fontSize: 24,
        width: 200,
        height: 30,
      },
    ]);
  };

  // Add Image from URL
  const addImage = () => {
    const url = prompt("Enter image URL");
    if (!url) return;

    setElements((p) => [
      ...p,
      {
        id: crypto.randomUUID(),
        type: "image",
        url,
        x: 140,
        y: 140,
        width: 200,
        height: 200,
      },
    ]);
  };

  // Drag move handler
  const onDragMove = (id, pos) => {
    setElements((prev) =>
      prev.map((el) => (el.id === id ? { ...el, ...pos } : el))
    );
    generateAlignmentGuides();
  };

  // Generate alignment guides
  const generateAlignmentGuides = () => {
    const g = [];
    elements.forEach((el) => {
      g.push({ x: el.x, type: "vertical" });
      g.push({ x: el.x + el.width, type: "vertical" });
      g.push({ y: el.y, type: "horizontal" });
      g.push({ y: el.y + el.height, type: "horizontal" });
    });
    setGuides(g);
  };

  // Check overlap
  const checkOverlap = () => {
    const errors = [];

    for (let i = 0; i < elements.length; i++) {
      for (let j = i + 1; j < elements.length; j++) {
        const A = elements[i];
        const B = elements[j];

        const isOverlap =
          A.x < B.x + B.width &&
          A.x + A.width > B.x &&
          A.y < B.y + B.height &&
          A.y + A.height > B.y;

        if (isOverlap) {
          errors.push(`Overlap found between ${A.id} and ${B.id}`);
        }
      }
    }

    if (errors.length === 0) {
      alert("No overlap detected ✔");
    } else {
      alert(errors.join("\n"));
    }
  };

  // Save canvas
  const handleSave = () => {
    const dataURL = stageRef.current.toDataURL();
    onSave && onSave(dataURL);
  };

  return (
    <div>
      {/* Toolbar */}
      <div style={{ marginBottom: 10 }}>
        <button onClick={addText}>Add Text</button>
        <button onClick={addImage}>Add Image URL</button>
        <button onClick={checkOverlap}>Check Overlap</button>
        <button onClick={handleSave}>Save</button>
        <button onClick={() => onCheckCompliance && onCheckCompliance(elements)}>
          Check Compliance
        </button>
      </div>

      {/* Canvas */}
      <Stage
        width={canvasSize.width}
        height={canvasSize.height}
        ref={stageRef}
        style={{ border: "1px solid black" }}
      >
        <Layer>
          {elements.map((el) =>
            el.type === "text" ? (
              <Text
                key={el.id}
                text={el.text}
                x={el.x}
                y={el.y}
                fontSize={el.fontSize}
                draggable
                onDragMove={(e) =>
                  onDragMove(el.id, {
                    x: e.target.x(),
                    y: e.target.y(),
                  })
                }
              />
            ) : (
              <URLImage
                key={el.id}
                el={el}
                onSelect={() => {}}
                onDragMove={onDragMove}
              />
            )
          )}

          {/* Alignment Guides */}
          {guides.map((g, i) =>
            g.type === "vertical" ? (
              <Text
                key={i}
                x={g.x}
                y={0}
                text="|"
                fill="red"
                fontSize={20}
              />
            ) : (
              <Text
                key={i}
                x={0}
                y={g.y}
                text="—"
                fill="red"
                fontSize={20}
              />
            )
          )}
        </Layer>
      </Stage>
    </div>
  );
}
