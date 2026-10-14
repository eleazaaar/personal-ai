package com.personalai.personal_ai.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final JdbcTemplate jdbcTemplate;

    public CustomUserDetailsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        return jdbcTemplate.query(
            """
            SELECT username, password_hash, role, enabled
            FROM users
            WHERE username = ?
            """,
            (rs, rowNum) -> User.withUsername(rs.getString("username"))
                .password(rs.getString("password_hash"))
                .roles(rs.getString("role"))
                .disabled(!rs.getBoolean("enabled"))
                .build(),
            username
        ).stream().findFirst().orElseThrow(
            () -> new UsernameNotFoundException("Account not found")
        );
    }
}
