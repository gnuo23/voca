ALTER TABLE vocab_items ADD COLUMN audio_url VARCHAR(1000);
ALTER TABLE vocab_items ADD COLUMN audio_us_url VARCHAR(1000);
ALTER TABLE vocab_items ADD COLUMN audio_uk_url VARCHAR(1000);
ALTER TABLE vocab_items ADD COLUMN audio_accent VARCHAR(20);
ALTER TABLE vocab_items ADD COLUMN audio_source VARCHAR(100);
ALTER TABLE vocab_items ADD COLUMN audio_refreshed_at TIMESTAMP;
