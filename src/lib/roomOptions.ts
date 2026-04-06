export const ROOM_CATEGORY_OPTIONS = [
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
    "Royal Suite",
] as const;

export const CLIMATE_CONTROL_OPTIONS = ["AC", "Non-AC"] as const;

export function roomCategoryBadge(category?: string) {
    return category?.trim() || "Standard Room";
}

export function climateControlBadge(climateControl?: string) {
    return climateControl?.trim() || "AC";
}
