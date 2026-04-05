CREATE TABLE IF NOT EXISTS launcher_store (
  store_key TEXT PRIMARY KEY,
  data JSONB NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS pending_activations (
  poll_token TEXT PRIMARY KEY,
  activation_code TEXT NOT NULL UNIQUE,
  device_id TEXT NOT NULL,
  mac_address TEXT,
  status TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS device_bindings (
  session_token TEXT PRIMARY KEY,
  poll_token TEXT NOT NULL UNIQUE,
  activation_code TEXT NOT NULL,
  room_number TEXT NOT NULL,
  device_id TEXT NOT NULL,
  mac_address TEXT,
  bound_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_users (
  id TEXT PRIMARY KEY,
  name TEXT NOT NULL,
  email TEXT NOT NULL UNIQUE,
  password_hash TEXT NOT NULL,
  role TEXT NOT NULL,
  status TEXT NOT NULL,
  property_id TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admin_sessions (
  session_token TEXT PRIMARY KEY,
  user_id TEXT NOT NULL REFERENCES admin_users(id) ON DELETE CASCADE,
  expires_at TIMESTAMPTZ NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

INSERT INTO launcher_store (store_key, data)
VALUES (
  'global',
  '{
    "hotel": {
      "hotelName": "Asteria Grand",
      "shortBrand": "AG",
      "tagline": "A premium guest-room experience across every screen",
      "location": "Dhaka",
      "supportPhone": "+880 1234 567890",
      "startupLogoUrl": null,
      "checkoutLabel": "Checkout 12:00 PM",
      "billLabel": "Total bill till now",
      "billValue": "Pending sync",
      "loadingMessage": "Preparing your room experience"
    },
    "weather": {
      "temperatureC": 28,
      "condition": "Clear"
    },
    "popup": {
      "ratingText": "4.8/5 guest rating",
      "callHint": "If you want you can call this number",
      "callNumber": "+880 1234 567890"
    },
    "backgrounds": {
      "home": [],
      "roomService": [],
      "foodMenu": [],
      "inputs": []
    },
    "meals": {
      "breakfast": [],
      "lunch": [],
      "dinner": []
    },
    "sections": {},
    "visibility": {
      "destinations": {
        "home": true,
        "roomService": true,
        "foodMenu": true,
        "inputs": true
      },
      "visibleAppPackages": [],
      "visibleSourceTitles": []
    },
    "rooms": {},
    "sync": {
      "version": 1,
      "ttlSeconds": 300
    }
  }'::jsonb
)
ON CONFLICT (store_key) DO NOTHING;
