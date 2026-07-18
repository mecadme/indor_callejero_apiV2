package com.indorcallejero.api.facebookvideo;

import jakarta.validation.constraints.NotBlank;

public record UpdateFacebookVideoRequest(
        @NotBlank String title,
        @NotBlank String videoUrl
) {
}
