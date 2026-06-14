package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.samples.petclinic.model.User;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JpaUserRepositoryImplTest {

    private EntityManager em;
    private JpaUserRepositoryImpl repository;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        em = mock(EntityManager.class);
        repository = new JpaUserRepositoryImpl();
        Field emField = JpaUserRepositoryImpl.class.getDeclaredField("em");
        emField.setAccessible(true);
        emField.set(repository, em);
    }

    @Test
    void save_userNotExists_shouldPersistUser() throws DataAccessException {
        // Arrange
        User newUser = new User();
        newUser.setUsername("newuser");
        when(em.find(User.class, "newuser")).thenReturn(null);

        // Act
        repository.save(newUser);

        // Assert
        verify(em).find(User.class, "newuser");
        verify(em).persist(newUser);
        verify(em, never()).merge(any());
        assertThat(newUser.getUsername()).isEqualTo("newuser");
    }

    @Test
    void save_userExists_shouldMergeUser() throws DataAccessException {
        // Arrange
        User existingUser = new User();
        existingUser.setUsername("existinguser");
        when(em.find(User.class, "existinguser")).thenReturn(existingUser);

        // Act
        repository.save(existingUser);

        // Assert
        verify(em).find(User.class, "existinguser");
        verify(em).merge(existingUser);
        verify(em, never()).persist(any());
        assertThat(existingUser.getUsername()).isEqualTo("existinguser");
    }
}
