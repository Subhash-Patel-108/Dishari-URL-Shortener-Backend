package com.dishari.in.application.serviceImpl;

import com.dishari.in.domain.entity.User;
import com.dishari.in.domain.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @NullMarked
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid email or password")) ;

        // most restrictive first
        if (user.isFrozen()) {
            throw new LockedException("Account frozen. Contact support.");
        }
        if (!user.isVerified()) {
            throw new DisabledException("Please verify your email before logging in.");
        }
        if (!user.isEnabled()) {
            throw new DisabledException("Account is disabled.");
        }
        return user;
    }
}
