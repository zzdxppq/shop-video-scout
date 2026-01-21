package com.shopvideoscout.user.dto;

import com.shopvideoscout.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Response DTO for successful login.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * User information (with masked phone).
     */
    private UserInfo user;

    /**
     * JWT access token.
     */
    private String accessToken;

    /**
     * JWT refresh token.
     */
    private String refreshToken;

    /**
     * Access token expiration time in seconds.
     */
    private Long expiresIn;

    /**
     * User info nested object.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserInfo {
        private Long id;
        private String phone;
        private String nickname;
        private String membershipType;

        public static UserInfo fromUser(User user) {
            return UserInfo.builder()
                    .id(user.getId())
                    .phone(user.getMaskedPhone())
                    .nickname(user.getNickname())
                    .membershipType(user.getMembershipType())
                    .build();
        }
    }
}
