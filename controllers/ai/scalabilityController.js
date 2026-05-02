const axios = require("axios");
const fs = require("fs");
const FormData = require("form-data");

const SCALABILITY_KEY = process.env.SCALABILITY_API_KEY;

//
// Scalability – Universal request wrapper
//
async function scalabilityRequest(endpoint, filePath) {
  const form = new FormData();
  form.append("image", fs.createReadStream(filePath));

  const result = await axios.post(
    `https://api.scalability.cloud/${endpoint}`,
    form,
    {
      headers: {
        "x-api-key": SCALABILITY_KEY,
        ...form.getHeaders(),
      },
      responseType: "arraybuffer",
    }
  );

  return result.data;
}

//
// 1) UPSCALE
//
exports.upscale = async (req, res) => {
  const file = req.file;
  const data = await scalabilityRequest("upscale", file.path);
  res.set("Content-Type", "image/png").send(data);
};

//
// 2) ENHANCE
//
exports.enhance = async (req, res) => {
  const file = req.file;
  const data = await scalabilityRequest("enhance", file.path);
  res.set("Content-Type", "image/png").send(data);
};

//
// 3) REMOVE BACKGROUND
//
exports.removeBackground = async (req, res) => {
  const file = req.file;
  const data = await scalabilityRequest("remove-bg", file.path);
  res.set("Content-Type", "image/png").send(data);
};

//
// 4) GENERATIVE FILL
//
exports.fill = async (req, res) => {
  const file = req.file;
  const data = await scalabilityRequest("fill", file.path);
  res.set("Content-Type", "image/png").send(data);
};

