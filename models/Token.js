const mongoose = require("mongoose");

const TokenSchema = new mongoose.Schema({
  userId: String,
  refreshToken: String,
  createdAt: { type: Date, default: Date.now },
});

module.exports = mongoose.model("Token", TokenSchema);

