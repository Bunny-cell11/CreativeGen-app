const axios = require("axios");
const FormData = require("form-data");
const fs = require("fs");

exports.removeBackgroundRemoveBG = async (req, res) => {
  try {
    const file = req.file;

    const form = new FormData();
    form.append("image_file", fs.createReadStream(file.path));
    form.append("size", "auto");

    const result = await axios.post(
      "https://api.remove.bg/v1.0/removebg",
      form,
      {
        headers: {
          "X-Api-Key": process.env.REMOVEBG_API_KEY,
          ...form.getHeaders(),
        },
        responseType: "arraybuffer",
      }
    );

    res.set("Content-Type", "image/png").send(result.data);
  } catch (err) {
    console.log(err.response?.data);
    res.status(500).json({ error: "Remove.bg failed" });
  }
};

