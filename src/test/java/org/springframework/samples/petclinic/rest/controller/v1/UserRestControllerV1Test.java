package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.UserMapper;
import org.springframework.samples.petclinic.model.User;
import org.springframework.samples.petclinic.rest.dto.UserDto;
import org.springframework.samples.petclinic.service.UserService;

class UserRestControllerV1Test {

    @Mock
    private UserService userService;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserRestControllerV1 userRestControllerV1;

    private UserDto userDto;
    private User user;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userDto = new UserDto();
        userDto.setUsername("testuser");
        userDto.setFirstName("Test");
        userDto.setLastName("User");
        userDto.setEmail("testuser@example.com");

        user = new User();
        user.setUsername("testuser");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setEmail("testuser@example.com");
    }

    @Test
    void addUser_validUserDto_returnsCreatedResponseWithUserDto() {
        // Arrange
        when(userMapper.toUser(userDto)).thenReturn(user);
        when(userMapper.toUserDto(user)).thenReturn(userDto);

        // Act
        ResponseEntity<UserDto> response = userRestControllerV1.addUser(userDto);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(userDto);
        verify(userMapper).toUser(userDto);
        verify(userService).saveUser(user);
        verify(userMapper).toUserDto(user);
    }
}
