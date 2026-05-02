import React, { useState } from 'react';
import api from '../services/api';
import { setToken, setUser } from '../services/auth';
import { useHistory } from "react-router-dom";

export default function Register(){
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const history = useHistory();
history.push('/login');

  async function submit(e){
    e.preventDefault();
    setErr('');
    try {
      const res = await api.post('/auth/register', { name, email, password });
      setToken(res.data.token);
      setUser(res.data.user);
      nav('/dashboard');
    } catch (er) {
      setErr(er?.response?.data?.msg || 'Registration failed');
    }
  }

  return (
    <div style={{maxWidth:420, margin:'40px auto'}}>
      <h2>Register</h2>
      <form onSubmit={submit}>
        <div style={{marginBottom:10}}>
          <label>Name</label><br/>
          <input value={name} onChange={e=>setName(e.target.value)} required />
        </div>
        <div style={{marginBottom:10}}>
          <label>Email</label><br/>
          <input value={email} onChange={e=>setEmail(e.target.value)} required />
        </div>
        <div style={{marginBottom:10}}>
          <label>Password</label><br/>
          <input type="password" value={password} onChange={e=>setPassword(e.target.value)} required />
        </div>
        {err && <div style={{color:'red', marginBottom:10}}>{err}</div>}
        <button type="submit">Register</button>
      </form>
    </div>
  );
}

