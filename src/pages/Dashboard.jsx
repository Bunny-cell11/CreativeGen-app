import React from 'react';
import { Link } from 'react-router-dom';

export default function Dashboard(){
  return (
    <div style={{padding:20}}>
      <h2>Dashboard</h2>
      <p>Create a new creative or open existing ones.</p>
      <div style={{marginTop:20}}>
        <Link to="/editor">New Creative</Link>
      </div>
    </div>
  );
}

