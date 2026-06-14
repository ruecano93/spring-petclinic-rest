package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.samples.petclinic.model.Role;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl userService;

    User user;

    @BeforeEach
    void setup() {
        user = new User();
    }

    @Test
    void saveUser_validUser_rolesPrefixedAndUserSetAndSaved() {
        // Arrange
        Role role1 = new Role();
        role1.setName("ADMIN");
        Role role2 = new Role();
        role2.setName("USER");
        Set<Role> roles = new HashSet<>();
        roles.add(role1);
        roles.add(role2);
        user.setRoles(roles);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.saveUser(user);

        // Assert
        assertThat(user.getRoles()).allSatisfy(role -> {
            assertThat(role.getName()).startsWith("ROLE_");
            assertThat(role.getUser()).isEqualTo(user);
        });
        verify(userRepository).save(user);
    }

    @Test
    void saveUser_userWithRolesWithPrefix_rolesNotModificadosAndSaved() {
        // Arrange
        Role role1 = new Role();
        role1.setName("ROLE_ADMIN");
        role1.setUser(user);
        Role role2 = new Role();
        role2.setName("ROLE_USER");
        role2.setUser(user);
        Set<Role> roles = new HashSet<>();
        roles.add(role1);
        roles.add(role2);
        user.setRoles(roles);

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        userService.saveUser(user);

        // Assert
        assertThat(user.getRoles()).allSatisfy(role -> {
            assertThat(role.getName()).startsWith("ROLE_");
            assertThat(role.getUser()).isEqualTo(user);
        });
        verify(userRepository).save(user);
    }

    @Test
    void saveUser_userWithNullRoles_throwsIllegalArgumentException() {
        // Arrange
        user.setRoles(null);

        // Act / Assert
        assertThatCode(() -> userService.saveUser(user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User must have at least a role set!");
    }

    @Test
    void saveUser_userWithEmptyRoles_throwsIllegalArgumentException() {
        // Arrange
        user.setRoles(new HashSet<>());

        // Act / Assert
        assertThatCode(() -> userService.saveUser(user))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User must have at least a role set!");
    }
}
