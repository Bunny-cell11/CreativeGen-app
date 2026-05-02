import { useEffect } from "react";
import { setToken } from "../services/auth";
import { useNavigate } from "react-router-dom";

export default function OAuthSuccess(){
  const nav = useNavigate();

  useEffect(() => {
    const query = new URLSearchParams(window.location.search);
    setToken(query.get("access"));
    localStorage.setItem("refresh", query.get("refresh"));

    nav("/dashboard");
  }, []);

  return <div>Redirecting...</div>;
}

