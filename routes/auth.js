const express = require("express");
const router = express.Router();
const jwt = require("jsonwebtoken");
const bcrypt = require("bcryptjs");

const store = require("../data/store");

// REGISTER
router.post("/register", async (req, res) => {
  const { name, email, password } = req.body;

  const exists = store.users.find((u) => u.email === email);
  if (exists) return res.status(400).json({ msg: "User already exists" });

  const hash = await bcrypt.hash(password, 10);
  const newUser = {
    id: Date.now().toString(),
    name,
    email,
    passwordHash: hash,
  };

  store.users.push(newUser);

  const token = jwt.sign({ id: newUser.id }, process.env.JWT_SECRET, {
    expiresIn: "15m",
  });

  const refresh = jwt.sign({ id: newUser.id }, process.env.JWT_SECRET, {
    expiresIn: "30d",
  });

  store.tokens.push({ userId: newUser.id, token: refresh });

  res.json({ token, refresh, user: newUser });
});

// LOGIN
router.post("/login", async (req, res) => {
  const { email, password } = req.body;

  const user = store.users.find((u) => u.email === email);
  if (!user) return res.status(400).json({ msg: "Invalid email" });

  const match = await bcrypt.compare(password, user.passwordHash);
  if (!match) return res.status(400).json({ msg: "Invalid password" });

  const token = jwt.sign({ id: user.id }, process.env.JWT_SECRET, {
    expiresIn: "15m",
  });

  const refresh = jwt.sign({ id: user.id }, process.env.JWT_SECRET, {
    expiresIn: "30d",
  });

  store.tokens.push({ userId: user.id, token: refresh });

  res.json({ token, refresh, user });
});

// REFRESH TOKEN
router.post("/refresh", (req, res) => {
  const { token } = req.body;

  const entry = store.tokens.find((t) => t.token === token);
  if (!entry) return res.status(403).json({ msg: "Invalid refresh token" });

  try {
    const decoded = jwt.verify(token, process.env.JWT_SECRET);

    const newAccess = jwt.sign({ id: decoded.id }, process.env.JWT_SECRET, {
      expiresIn: "15m",
    });

    res.json({ accessToken: newAccess });
  } catch (err) {
    return res.status(403).json({ msg: "Token expired" });
  }
});

module.exports = router;

