const ROOM_CATEGORY_OPTIONS = [
  "Standard Room",
  "Deluxe Room",
  "Executive Room",
  "Family Room",
  "Junior Suite",
  "Deluxe Suite",
  "Executive Suite",
  "Family Suite",
  "Honeymoon Suite",
  "Presidential Suite",
  "Royal Suite"
];

const CLIMATE_CONTROL_OPTIONS = ["AC", "Non-AC"];

function normalizeRoomCategory(value) {
  const candidate = `${value || ""}`.trim();
  return ROOM_CATEGORY_OPTIONS.includes(candidate) ? candidate : ROOM_CATEGORY_OPTIONS[0];
}

function normalizeClimateControl(value) {
  const candidate = `${value || ""}`.trim();
  return CLIMATE_CONTROL_OPTIONS.includes(candidate) ? candidate : CLIMATE_CONTROL_OPTIONS[0];
}

module.exports = {
  ROOM_CATEGORY_OPTIONS,
  CLIMATE_CONTROL_OPTIONS,
  normalizeRoomCategory,
  normalizeClimateControl
};
