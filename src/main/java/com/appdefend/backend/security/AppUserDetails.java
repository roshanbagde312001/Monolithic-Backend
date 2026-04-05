package com.appdefend.backend.security;

import com.appdefend.backend.model.PlatformUser;
import java.util.Collection;
import java.util.stream.Stream;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public class AppUserDetails implements UserDetails {
    private final PlatformUser user;

    public AppUserDetails(PlatformUser user) {
        this.user = user;
    }

    public PlatformUser getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Stream.concat(
            user.roles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)),
            user.permissions().stream().map(SimpleGrantedAuthority::new)
        ).toList();
    }

    @Override
    public String getPassword() {
        return user.passwordHash();
    }

    @Override
    public String getUsername() {
        return user.email();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.enabled();
    }
}
