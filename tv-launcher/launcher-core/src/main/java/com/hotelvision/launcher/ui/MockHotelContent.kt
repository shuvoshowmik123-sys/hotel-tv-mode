package com.hotelvision.launcher.ui

object MockHotelContent {
    val branding = HotelBranding(
        hotelName = "Asteria Grand Dhaka",
        shortBrand = "AG",
        tagline = "Hotel & Residences",
        location = "Banani, Dhaka"
    )

    val defaultBackdrop = AmbientBackdropState(
        title = branding.hotelName,
        imageUrl = "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1800&q=80",
        slideshowImages = listOf(
            "https://images.unsplash.com/photo-1445019980597-93fa8acb246c?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1522798514-97ceb8c4f1c8?auto=format&fit=crop&w=1800&q=80",
            "https://images.unsplash.com/photo-1512918728675-ed5a9ecdebfd?auto=format&fit=crop&w=1800&q=80"
        )
    )

    private val breakfastCards = listOf(
        HotelFeatureCard(
            id = "breakfast_signature",
            title = "Sunrise Breakfast",
            subtitle = "Continental and Bengali selections",
            supportingText = "Fresh bakery, fruit station, local specialties, and barista service.",
            imageUrl = "https://images.unsplash.com/photo-1533089860892-a7c6f0a88666?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1504754524776-8f4f37790ca0?auto=format&fit=crop&w=1800&q=80",
            badge = "AM",
            accentColor = 0xFFD79B47
        ),
        HotelFeatureCard(
            id = "breakfast_suite",
            title = "Suite Morning Tray",
            subtitle = "Delivered to your room",
            supportingText = "Private breakfast with tea service, tropical fruit, and eggs to order.",
            imageUrl = "https://images.unsplash.com/photo-1525351484163-7529414344d8?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1495474472287-4d71bcdd2085?auto=format&fit=crop&w=1800&q=80",
            badge = "DIN",
            accentColor = 0xFFC9872C
        ),
        HotelFeatureCard(
            id = "breakfast_health",
            title = "Wellness Breakfast",
            subtitle = "Lighter morning menu",
            supportingText = "Cold-pressed juices, yogurt bowls, and chef-crafted healthy plates.",
            imageUrl = "https://images.unsplash.com/photo-1512621776951-a57141f2eefd?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1467453678174-768ec283a940?auto=format&fit=crop&w=1800&q=80",
            badge = "FIT",
            accentColor = 0xFFB8772C
        )
    )

    private val lunchCards = listOf(
        HotelFeatureCard(
            id = "lunch_club",
            title = "Executive Lunch",
            subtitle = "Chef's midday plates",
            supportingText = "Business lunch favorites, grilled mains, and signature desserts.",
            imageUrl = "https://images.unsplash.com/photo-1544025162-d76694265947?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1800&q=80",
            badge = "LUN",
            accentColor = 0xFFD48833
        ),
        HotelFeatureCard(
            id = "lunch_bistro",
            title = "Poolside Bistro",
            subtitle = "Light meals and iced drinks",
            supportingText = "Flatbreads, grilled seafood, and seasonal coolers served poolside.",
            imageUrl = "https://images.unsplash.com/photo-1552566626-52f8b828add9?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1551882547-ff40c63fe5fa?auto=format&fit=crop&w=1800&q=80",
            badge = "SUN",
            accentColor = 0xFFC87A2C
        ),
        HotelFeatureCard(
            id = "lunch_room",
            title = "In-Room Lunch",
            subtitle = "Quiet dining in your suite",
            supportingText = "Fast premium dining for meetings, rest, or family time in the room.",
            imageUrl = "https://images.unsplash.com/photo-1528605248644-14dd04022da1?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=1800&q=80",
            badge = "VIP",
            accentColor = 0xFFB56E24
        )
    )

    private val dinnerCards = listOf(
        HotelFeatureCard(
            id = "dinner_rooftop",
            title = "Rooftop Grill",
            subtitle = "Open until 11:30 PM",
            supportingText = "Steaks, seafood platters, and skyline dining with lounge music.",
            imageUrl = "https://images.unsplash.com/photo-1559339352-11d035aa65de?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=1800&q=80",
            badge = "PM",
            accentColor = 0xFFCF8E38
        ),
        HotelFeatureCard(
            id = "dinner_tasting",
            title = "Chef's Tasting",
            subtitle = "Seven-course signature dinner",
            supportingText = "An elevated tasting experience reserved for in-house guests.",
            imageUrl = "https://images.unsplash.com/photo-1517248135467-4c7edcad34c4?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1466978913421-dad2ebd01d17?auto=format&fit=crop&w=1800&q=80",
            badge = "VIP",
            accentColor = 0xFFBB7C2C
        ),
        HotelFeatureCard(
            id = "dinner_lounge",
            title = "Lounge Supper",
            subtitle = "Late-night small plates",
            supportingText = "Cocktails, desserts, and small plates in the executive lounge.",
            imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1470337458703-46ad1756a187?auto=format&fit=crop&w=1800&q=80",
            badge = "EVE",
            accentColor = 0xFFA96C26
        )
    )

    private val lateNightCards = listOf(
        HotelFeatureCard(
            id = "late_supper",
            title = "Midnight Supper",
            subtitle = "Lounge favourites",
            supportingText = "Small plates, artisanal cheese, and late-night selects available until midnight.",
            imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1800&q=80",
            badge = "LATE",
            accentColor = 0xFF5A6FAE
        ),
        HotelFeatureCard(
            id = "late_drinks",
            title = "Nightcap Service",
            subtitle = "Beverage selections",
            supportingText = "Premium tea service, craft cocktails, and non-alcoholic recovery drinks.",
            imageUrl = "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=900&q=80",
            ambientImageUrl = "https://images.unsplash.com/photo-1514362545857-3bc16c4c7d1b?auto=format&fit=crop&w=1800&q=80",
            badge = "PM",
            accentColor = 0xFF4A5F9E
        )
    )

    fun diningRecommendations(mealPeriod: MealPeriod): HotelFeatureSection? {
        val cards = when (mealPeriod) {
            MealPeriod.BREAKFAST -> breakfastCards
            MealPeriod.LUNCH -> lunchCards
            MealPeriod.DINNER -> dinnerCards
            MealPeriod.LATE_NIGHT -> lateNightCards
            MealPeriod.OFF_HOURS -> return null  // hide food row outside meal windows
        }
        val title = when (mealPeriod) {
            MealPeriod.BREAKFAST -> "🍳 Breakfast Menu"
            MealPeriod.LUNCH -> "☀️ Lunch Menu"
            MealPeriod.DINNER -> "🌙 Dinner Menu"
            MealPeriod.LATE_NIGHT -> "🌌 Late Night Menu"
            MealPeriod.OFF_HOURS -> return null
        }
        val subtitle = when (mealPeriod) {
            MealPeriod.BREAKFAST -> "Morning dining curated for guests starting the day · 06:00 – 11:00"
            MealPeriod.LUNCH -> "Midday favourites for business and leisure stays · 11:00 – 15:00"
            MealPeriod.DINNER -> "Evening dining and lounge experiences for tonight · 18:00 – 22:00"
            MealPeriod.LATE_NIGHT -> "Supper and midnight snacks for night owls · 22:00 – 00:00"
            MealPeriod.OFF_HOURS -> return null
        }

        return HotelFeatureSection(
            id = "dining_${mealPeriod.name.lowercase()}",
            title = title,
            subtitle = subtitle,
            style = HomeSectionStyle.STANDARD,
            cards = cards
        )
    }

    fun hotelFeatureSections(): List<HotelFeatureSection> {
        return listOf(
            HotelFeatureSection(
                id = "spa",
                title = "Spa & Wellness",
                subtitle = "Recovery and wellness experiences available at Asteria Grand",
                style = HomeSectionStyle.STANDARD,
                cards = listOf(
                    HotelFeatureCard(
                        id = "spa_signature",
                        title = "Signature Therapy",
                        subtitle = "90-minute recovery ritual",
                        supportingText = "Deep relaxation treatment with tea service and private lounge access.",
                        imageUrl = "https://images.unsplash.com/photo-1515377905703-c4788e51af15?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1544161515-4ab6ce6db874?auto=format&fit=crop&w=1800&q=80",
                        badge = "SPA",
                        accentColor = 0xFF4B9F96
                    ),
                    HotelFeatureCard(
                        id = "spa_sauna",
                        title = "Steam & Sauna",
                        subtitle = "Open daily from 7 AM",
                        supportingText = "Quiet wellness floor with steam, sauna, and guided recovery options.",
                        imageUrl = "https://images.unsplash.com/photo-1519823551278-64ac92734fb1?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1519823551278-64ac92734fb1?auto=format&fit=crop&w=1800&q=80",
                        badge = "ZEN",
                        accentColor = 0xFF33877D
                    ),
                    HotelFeatureCard(
                        id = "spa_fitness",
                        title = "Fitness Studio",
                        subtitle = "Trainer-led sessions available",
                        supportingText = "Cardio, stretch, and strength sessions tailored to guest schedules.",
                        imageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=1800&q=80",
                        badge = "FIT",
                        accentColor = 0xFF2A7068
                    )
                )
            ),
            HotelFeatureSection(
                id = "experiences",
                title = "Experiences",
                subtitle = "Property-curated discovery beyond the room",
                style = HomeSectionStyle.STANDARD,
                cards = listOf(
                    HotelFeatureCard(
                        id = "exp_city",
                        title = "Old Dhaka Evenings",
                        subtitle = "Private guided city route",
                        supportingText = "Reserved transport, guide support, and flexible evening departures.",
                        imageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1514565131-fce0801e5785?auto=format&fit=crop&w=1800&q=80",
                        badge = "EXP",
                        accentColor = 0xFF5A7CDA
                    ),
                    HotelFeatureCard(
                        id = "exp_shopping",
                        title = "Luxury Shopping Desk",
                        subtitle = "Curated retail recommendations",
                        supportingText = "Personalized itineraries for premium shopping and gifting requests.",
                        imageUrl = "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1523381210434-271e8be1f52b?auto=format&fit=crop&w=1800&q=80",
                        badge = "VIP",
                        accentColor = 0xFF6A82E3
                    ),
                    HotelFeatureCard(
                        id = "exp_evening",
                        title = "Executive Lounge Nights",
                        subtitle = "Cocktails and live jazz",
                        supportingText = "Access to the lounge program with evening music and chef tasting plates.",
                        imageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1516450360452-9312f5e86fc7?auto=format&fit=crop&w=1800&q=80",
                        badge = "EVE",
                        accentColor = 0xFF4E69C7
                    )
                )
            ),
            HotelFeatureSection(
                id = "offers",
                title = "Offers",
                subtitle = "Current property-only packages and premium upgrades",
                style = HomeSectionStyle.COMPACT,
                cards = listOf(
                    HotelFeatureCard(
                        id = "offer_suite",
                        title = "Suite Upgrade",
                        subtitle = "Executive floor access",
                        supportingText = "Upgrade tonight with lounge privileges and city-view seating.",
                        imageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1800&q=80",
                        badge = "UP",
                        accentColor = 0xFF9C6CE7
                    ),
                    HotelFeatureCard(
                        id = "offer_brunch",
                        title = "Weekend Brunch",
                        subtitle = "Guest-exclusive table pricing",
                        supportingText = "Reserve brunch with live stations and premium dessert service.",
                        imageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1414235077428-338989a2e8c0?auto=format&fit=crop&w=1800&q=80",
                        badge = "BRN",
                        accentColor = 0xFFC46B78
                    ),
                    HotelFeatureCard(
                        id = "offer_transfer",
                        title = "Airport Transfer",
                        subtitle = "Luxury sedan or van",
                        supportingText = "Secure return transfer with meet-and-assist service.",
                        imageUrl = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?auto=format&fit=crop&w=1800&q=80",
                        badge = "CAR",
                        accentColor = 0xFF62758E
                    )
                )
            ),
            HotelFeatureSection(
                id = "services",
                title = "Guest Services",
                subtitle = "Dedicated support configured for this hotel",
                style = HomeSectionStyle.COMPACT,
                cards = listOf(
                    HotelFeatureCard(
                        id = "service_housekeeping",
                        title = "Housekeeping",
                        subtitle = "Freshen up your room",
                        supportingText = "Request additional towels, turndown, or room refresh service.",
                        imageUrl = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1582719478250-c89cae4dc85b?auto=format&fit=crop&w=1800&q=80",
                        badge = "CLN",
                        accentColor = 0xFF2A8F84
                    ),
                    HotelFeatureCard(
                        id = "service_transport",
                        title = "Transport Desk",
                        subtitle = "Airport and city transfers",
                        supportingText = "Arrange hotel vehicles, drivers, and pickup windows.",
                        imageUrl = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1449965408869-eaa3f722e40d?auto=format&fit=crop&w=1800&q=80",
                        badge = "DRV",
                        accentColor = 0xFF337D97
                    ),
                    HotelFeatureCard(
                        id = "service_laundry",
                        title = "Laundry Express",
                        subtitle = "Same-day pressing available",
                        supportingText = "Fast turnaround for business attire, family wear, and special garments.",
                        imageUrl = "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=900&q=80",
                        ambientImageUrl = "https://images.unsplash.com/photo-1517677208171-0bc6725a3e60?auto=format&fit=crop&w=1800&q=80",
                        badge = "LND",
                        accentColor = 0xFF3A7086
                    )
                )
            )
        )
    }
}
