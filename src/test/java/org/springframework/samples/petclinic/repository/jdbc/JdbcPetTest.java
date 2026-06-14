package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class JdbcPetTest {

    @Test
    void getTypeId_defaultValue_zero() {
        // Arrange
        JdbcPet pet = new JdbcPet();

        // Act
        int typeId = pet.getTypeId();

        // Assert
        assertThat(typeId).isZero();
    }

    @Test
    void setTypeId_validValue_typeIdIsSet() {
        // Arrange
        JdbcPet pet = new JdbcPet();
        int expectedTypeId = 5;

        // Act
        pet.setTypeId(expectedTypeId);
        int actualTypeId = pet.getTypeId();

        // Assert
        assertThat(actualTypeId).isEqualTo(expectedTypeId);
    }

    @Test
    void getOwnerId_defaultValue_zero() {
        // Arrange
        JdbcPet pet = new JdbcPet();

        // Act
        int ownerId = pet.getOwnerId();

        // Assert
        assertThat(ownerId).isZero();
    }

    @Test
    void setOwnerId_validValue_ownerIdIsSet() {
        // Arrange
        JdbcPet pet = new JdbcPet();
        int expectedOwnerId = 10;

        // Act
        pet.setOwnerId(expectedOwnerId);
        int actualOwnerId = pet.getOwnerId();

        // Assert
        assertThat(actualOwnerId).isEqualTo(expectedOwnerId);
    }

}
