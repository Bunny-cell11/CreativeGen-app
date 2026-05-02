import React from 'react';
import { Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import Dashboard from './pages/Dashboard';
import Editor from './pages/Editor';
import ProtectedRoute from './routes/ProtectedRoute';
import Navbar from './components/Navbar';

export default function App() {
  return (
    <div>
      <Navbar />
      <Routes>
        <Route path="/" element={<Login />} />
        <Route path="/register" element={<Register />} />

        <Route path="/dashboard" element={
          <ProtectedRoute>
            <Dashboard />
          </ProtectedRoute>
        }/>

        <Route path="/editor/:id?" element={
          <ProtectedRoute>
            <Editor />
          </ProtectedRoute>
        }/>
      </Routes>
    </div>
  );
}

