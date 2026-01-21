package com.shopvideoscout.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.shopvideoscout.mybatis.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * User entity mapping to 'users' table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {

    /**
     * Phone number (unique identifier for login).
     */
    private String phone;

    /**
     * User display name.
     */
    private String nickname;

    /**
     * Avatar image URL.
     */
    private String avatarUrl;

    /**
     * Membership tier (free, basic, pro).
     */
    private String membershipType;

    /**
     * Create a new user with default values (BR-2.4: new users default to free).
     */
    public static User createNewUser(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickname("用户" + phone.substring(phone.length() - 6));
        user.setMembershipType("free");
        return user;
    }

    /**
     * Mask phone number for display (e.g., 138****8000).
     */
    public String getMaskedPhone() {
        if (phone == null || phone.length() < 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
