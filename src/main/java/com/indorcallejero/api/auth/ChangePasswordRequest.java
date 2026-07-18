package com.indorcallejero.api.auth;

public record ChangePasswordRequest(String oldPassword, String newPassword) {
}
