const TOKEN_KEY = 'creativegen_token';
const USER_KEY = 'creativegen_user';

export function setToken(token) {
  localStorage.setItem(TOKEN_KEY, token);
}
export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}
export function setUser(user) {
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}
export function getUser() {
  const v = localStorage.getItem(USER_KEY);
  return v ? JSON.parse(v) : null;
}
export function logout() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
  window.location.href = '/';
}

