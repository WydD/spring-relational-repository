CREATE TABLE Country (country_id VARCHAR(255) PRIMARY KEY, name VARCHAR(255), capital_id INT);
CREATE TABLE Location (country_id VARCHAR(255) REFERENCES Country, location_id INT AUTO_INCREMENT, name VARCHAR(255), PRIMARY KEY (country_id, location_id));
ALTER TABLE Country ADD FOREIGN KEY (country_id, capital_id) REFERENCES Location (country_id, location_id) ON DELETE CASCADE;
CREATE TABLE Event (id INT PRIMARY KEY AUTO_INCREMENT, name VARCHAR(255), country_id VARCHAR(255), location_id INT, FOREIGN KEY (country_id, location_id) REFERENCES Location);