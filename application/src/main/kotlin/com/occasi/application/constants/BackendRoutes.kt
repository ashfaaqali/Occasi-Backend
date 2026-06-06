package com.occasi.application.constants

object BackendRoutes {
    object Auth {
        const val BASE = "/auth"
        const val SEND_OTP = "/send-otp"
        const val VERIFY_OTP = "/verify-otp"
        const val VERIFY_PHONE = "/verify-phone"
        const val SEND_EMAIL_OTP = "/send-email-otp"
        const val VERIFY_EMAIL = "/verify-email"
        const val GOOGLE = "/google"
        const val REFRESH = "/refresh"
        const val LOGOUT = "/logout"
    }

    object ArtistAuth {
        const val BASE = "/artist-auth"
        const val LOGIN = "/login"
        const val REGISTER = "/register"
        const val SEND_EMAIL_OTP = "/send-email-otp"
        const val VERIFY_EMAIL_OTP = "/verify-email-otp"
        const val SEND_PHONE_OTP = "/send-phone-otp"
        const val VERIFY_PHONE_OTP = "/verify-phone-otp"
        const val FORGOT_PASSWORD = "/forgot-password"
        const val RESET_PASSWORD = "/reset-password"
        const val REFRESH = "/refresh"
        const val LOGOUT = "/logout"
    }

    object Bookings {
        const val BASE = "/bookings"
        const val BY_ID = "/{id}"
        const val VERIFY_PAYMENT = "/{id}/verify-payment"
        const val CANCEL = "/{id}/cancel"
        const val STATUS = "/{id}/status"
        const val BY_USER = "/user/{userId}"
        const val BY_ARTIST = "/artist/{artistId}"
        const val UPDATE_DESIGN = "/{id}/design"
        const val VERIFY_DIFF_PAYMENT = "/{id}/verify-diff-payment"
        const val COMPLETE = "/{id}/complete"
    }

    object HennaArtists {
        const val BASE = "/henna-artists"
        const val BY_ID = "/{id}"
    }

    object HennaDesigns {
        const val BASE = "/designs"
        const val BY_ID = "/{id}"
        const val BY_COMPLEXITY = "/complexity/{level}"
    }

    object Images {
        const val BASE = "/api/images"
        const val UPLOAD = "/upload"
    }

    object Favourites {
        const val BASE = "/api/favourites"
    }

    object InvitationCards {
        const val BASE = "/invitation-cards"
        const val BY_ID = "/{id}"
    }

    object Tags {
        const val BASE = "/tags"
    }

    object CardOrders {
        const val BASE = "/card-orders"
        const val BY_ID = "/{orderId}"
        const val VERIFY_PAYMENT = "/{orderId}/verify-payment"
        const val STATUS = "/{orderId}/status"
        const val BY_CUSTOMER = "/customer/{customerId}"
        const val SAMPLE_CHECK = "/sample-check"
    }

    object CardReviews {
        const val BASE = "/invitation-cards/{cardId}/reviews"
    }

    object ArtistPortfolio {
        const val BASE = "/artist-auth"
        const val PORTFOLIO = "/portfolio"
    }

    object ArtistPricing {
        const val BASE = "/artist-auth"
        const val PRICING = "/pricing"
    }

    object ArtistProfile {
        const val BASE = "/artist-auth"
        const val COVER_IMAGE = "/cover-image"
    }

    object Users {
        const val BASE = "/users"
        const val BY_ID = "/{id}"
        const val PROFILE = "/{id}/profile"
    }
}
