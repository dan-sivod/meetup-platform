rootProject.name = "meetup-platform"

include(
    "common",
    "services:gateway",
    "services:auth-service",
    "services:users-service",
    "services:friends-service",
    "services:meetups-service",
    "services:scheduling-service",
    "services:places-service",
    "services:chat-service",
    "services:notifications-service",
    "services:media-service",
    "services:recommendations-service",
    "services:search-service",
)
