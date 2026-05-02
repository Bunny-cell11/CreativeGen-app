import React, { useState, useRef } from 'react';
import { v4 as uuidv4 } from 'uuid';

function DraggableElement({ el, onUpdate, onSelect }) {
  const ref = useRef();
  const onMouseDown = (e) => {
    onSelect(el.id);
    ref.current.dataset.dragging = 'true';
    ref.current.dataset.offsetX = e.nativeEvent.offsetX;
    ref.current.dataset.offsetY = e.nativeEvent.offsetY;
  };
  const onMouseUp = () => {
    ref.current.dataset.dragging = 'false';
  };
  const onMouseMove = (e) => {
    if (ref.current?.dataset?.dragging === 'true') {
      const parentRect = ref.current.parentElement.getBoundingClientRect();
      const newX = e.clientX - parentRect.left - Number(ref.current.dataset.offsetX);
      const newY = e.clientY - parentRect.top - Number(ref.current.dataset.offsetY);
      onUpdate(el.id, { x: newX, y: newY });
    }
  };

  return (
    <div
      ref={ref}
      onMouseDown={onMouseDown}
      onMouseUp={onMouseUp}
      onMouseMove={onMouseMove}
      style={{
        position: 'absolute',
        left: el.x,
        top: el.y,
        cursor: 'move',
        border: '1px dashed rgba(0,0,0,0.2)',
        padding: 4,
        userSelect: 'none'
      }}
    >
      {el.type === 'text' ? (
        <div contentEditable suppressContentEditableWarning onBlur={(e)=>onUpdate(el.id, { text: e.target.textContent })}>
          {el.text}
        </div>
      ) : (
        <img src={el.url} alt="" style={{ width: el.width || 100, height: el.height || 'auto' }} />
      )}
    </div>
  );
}

export default function CanvasEditor({ onSave, onCheckCompliance }) {
  const [elements, setElements] = useState([]);
  const [selectedId, setSelectedId] = useState(null);

  const addText = () => {
    setElements(prev => [...prev, {
      id: uuidv4(),
      type: 'text',
      text: 'Sample text',
      x: 20, y: 20, width: 120, height: 30
    }]);
  };

  const addImageFromUrl = () => {
    const url = prompt('Image URL');
    if (!url) return;
    setElements(prev => [...prev, {
      id: uuidv4(), type: 'image', url, x: 40, y: 40, width: 160, height: 120
    }]);
  };

  const updateElement = (id, patch) => {
    setElements(prev => prev.map(e => e.id === id ? { ...e, ...patch } : e));
  };

  const onSaveClick = async () => {
    await onSave(elements);
  };

  const onCheckClick = async () => {
    const res = await onCheckCompliance({ elements });
    if (res.ok) alert('No compliance issues');
    else alert('Issues: ' + JSON.stringify(res.issues));
  };

  return (
    <div>
      <div style={{marginBottom:10}}>
        <button onClick={addText}>Add Text</button>
        <button onClick={addImageFromUrl}>Add Image (URL)</button>
        <button onClick={onSaveClick}>Save</button>
        <button onClick={onCheckClick}>Check Compliance</button>
      </div>

      <div id="canvas" style={{position:'relative', width:600, height:800, border:'1px solid #ddd', background:'#fff'}}>
        {elements.map(el => (
          <DraggableElement key={el.id} el={el} onUpdate={updateElement} onSelect={setSelectedId} />
        ))}
      </div>
    </div>
  );
}

