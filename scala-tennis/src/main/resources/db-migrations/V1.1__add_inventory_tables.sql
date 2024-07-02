CREATE TABLE court (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  description TEXT NOT NULL,
  weight_in_grams INT NOT NULL
);

CREATE TABLE court_availability (
  court_id TEXT PRIMARY KEY,
  quantity INT NOT NULL
);
