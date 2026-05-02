import React from "react";
import { BrowserRouter, Routes, Route } from "react-router-dom";

import Login from "./pages/Login";
import Register from "./pages/Register";
import Editor from "./pages/Editor";
import Dashboard from "./pages/Dashboard";

export default function App() {
  return (
    <Router>
      <Switch>
        <Route exact path="/" component={Login} />
        <Route path="/login" component={Login} />
        <Route path="/register" component={Register} />
        <Route path="/editor" component={Editor} />
        <Route path="/dashboard" component={Dashboard} />
      </Switch>
    </Router>
  );
}
