package com.indorcallejero.api.facebookvideo;

import jakarta.validation.constraints.NotBlank;

public record CreateFacebookVideoRequest(
        @NotBlank String title,
        @NotBlank String videoUrl
) {
}
