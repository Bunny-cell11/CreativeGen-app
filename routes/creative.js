const express = require('express');
const router = express.Router();
const auth = require('../middleware/auth');
const {
  getAllCreatives,
  getCreativeById,
  createCreative,
  updateCreative,
  deleteCreative
} = require('../controllers/creativeController');

// Routes for creatives
router.get('/', auth, getAllCreatives);          // GET all creatives
router.get('/:id', auth, getCreativeById);      // GET creative by ID
router.post('/', auth, createCreative);         // CREATE new creative
router.put('/:id', auth, updateCreative);       // UPDATE creative
router.delete('/:id', auth, deleteCreative);    // DELETE creative

module.exports = router;
