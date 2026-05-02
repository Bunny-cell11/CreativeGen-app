import React from 'react';
import { Link } from 'react-router-dom';
import { getUser, logout } from '../services/auth';

export default function Navbar(){
  const user = getUser();
  return (
    <nav style={{padding:'10px', borderBottom:'1px solid #eee', display:'flex', justifyContent:'space-between'}}>
      <div><Link to="/dashboard">CreativeGen</Link></div>
      <div>
        {user ? (
          <>
            <span style={{marginRight:12}}>Hi, {user.name}</span>
            <button onClick={logout}>Logout</button>
          </>
        ) : (
          <>
            <Link to="/">Login</Link> | <Link to="/register">Register</Link>
          </>
        )}
      </div>
    </nav>
  );
}

