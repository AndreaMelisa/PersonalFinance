package com.aaryav.finance.config;

import com.aaryav.finance.entity.User;
import com.aaryav.finance.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;
    @InjectMocks private CustomUserDetailsService service;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = User.builder()
                .id(1L)
                .username("bryan@test.com")
                .password("encodedPass")
                .fullName("Bryan Cacsire")
                .phoneNumber("999999999")
                .build();
        when(userRepository.findByUsername("bryan@test.com")).thenReturn(Optional.of(user));

        UserDetails details = service.loadUserByUsername("bryan@test.com");

        assertThat(details.getUsername()).isEqualTo("bryan@test.com");
        assertThat(details.getPassword()).isEqualTo("encodedPass");
        assertThat(details.getAuthorities()).isEmpty();
    }

    @Test
    void loadUserByUsername_unknownUser_throwsUsernameNotFound() {
        when(userRepository.findByUsername("ghost@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost@test.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("ghost@test.com");
    }
}