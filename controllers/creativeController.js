const Creative = require('../models/Creative');

// Get all creatives
exports.getAllCreatives = async (req, res) => {
  try {
    const creatives = await Creative.find({ user: req.user.id });
    res.json(creatives);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Get creative by ID
exports.getCreativeById = async (req, res) => {
  try {
    const creative = await Creative.findById(req.params.id);
    if (!creative) return res.status(404).json({ message: 'Creative not found' });
    res.json(creative);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Create new creative
exports.createCreative = async (req, res) => {
  try {
    const { title, content } = req.body;
    const newCreative = new Creative({
      title,
      content,
      user: req.user.id
    });
    await newCreative.save();
    res.status(201).json(newCreative);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Update creative
exports.updateCreative = async (req, res) => {
  try {
    const { title, content } = req.body;
    const updatedCreative = await Creative.findByIdAndUpdate(
      req.params.id,
      { title, content },
      { new: true }
    );
    if (!updatedCreative) return res.status(404).json({ message: 'Creative not found' });
    res.json(updatedCreative);
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
};

// Delete creative
exports.deleteCreative = async (req, res) => {
  try {
    const deletedCreative = await Creative.findByIdAndDelete(req.params.id);
    if (!deletedCreative) return res.status(404).json({ message: 'Creative not found' });
    res.json({ message: 'Creative deleted successfully' });
  } catch (error) {
    console.error(error);
    res.status(500).json({ message: 'Server error' });
  }
};

