package com.sungjujjang.entry.Global;

import com.sungjujjang.entry.Auth.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

public class CustomUserDetail implements UserDetails {

    private User user;
    private String phone;

    public CustomUserDetail(User user) {
        this.user = user;
        this.phone = user.getPhone();
    }

    public CustomUserDetail(String phone) {
        this.phone = phone;
    }

    public Long getId() { return user != null ? user.getId() : null; }

    @Override
    public String getUsername() { return user != null ? user.getName() : phone; }

    public String getPhone() { return phone; }

    @Override
    public String getPassword() { return user != null ? user.getPassword() : null; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }
}
