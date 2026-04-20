-- Fix rating column type: Java int maps to INTEGER, not TINYINT.
ALTER TABLE reviews MODIFY COLUMN rating INT NOT NULL;
