import React, { useState } from 'react';
import api from '../services/api';
import { setToken, setUser } from '../services/auth';
import { useHistory } from "react-router-dom";

export default function Login(){
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [err, setErr] = useState('');
  const history = useHistory();
  history.push('/home');  

  async function submit(e){
    e.preventDefault();
    setErr('');
    try {
      const res = await api.post('/auth/login', { email, password });
      setToken(res.data.token);
      setUser(res.data.user);
      nav('/dashboard');
    } catch (er) {
      setErr(er?.response?.data?.msg || 'Login failed');
    }
  }

  return (
    <div style={{maxWidth:420, margin:'40px auto'}}>
      <h2>Login</h2>
      <form onSubmit={submit}>
        <div style={{marginBottom:10}}>
          <label>Email</label><br/>
          <input value={email} onChange={e=>setEmail(e.target.value)} required />
        </div>
        <div style={{marginBottom:10}}>
          <label>Password</label><br/>
          <input type="password" value={password} onChange={e=>setPassword(e.target.value)} required />
        </div>
        {err && <div style={{color:'red', marginBottom:10}}>{err}</div>}
        <button type="submit">Login</button>
      </form>
    </div>
  );
}

