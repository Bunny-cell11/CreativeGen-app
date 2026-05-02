const axios = require("axios");
const FormData = require("form-data");
const fs = require("fs");

exports.removeBackground = async (req, res) => {
  const file = req.file;

  const form = new FormData();
  form.append("image_file", fs.createReadStream(file.path));

  const response = await axios.post("https://api.remove.bg/v1.0/removebg", form, {
    headers: {
      ...form.getHeaders(),
      "X-Api-Key": process.env.REMOVE_BG_KEY,
    },
    responseType: "arraybuffer",
  });

  const outPath = file.path.replace(".png", "-no-bg.png");
  fs.writeFileSync(outPath, response.data);

  res.json({ url: `/uploads/${outPath.split("/").pop()}` });
};

