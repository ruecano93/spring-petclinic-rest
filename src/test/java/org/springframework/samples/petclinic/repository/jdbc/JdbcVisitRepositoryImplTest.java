package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;

class JdbcVisitRepositoryImplTest {

    @Mock
    DataSource dataSource;

    @Mock
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    SimpleJdbcInsert insertVisit;

    JdbcVisitRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Instantiate repository with real dataSource mock (to avoid constructor errors)
        repository = new JdbcVisitRepositoryImpl(dataSource);
        // Replace internal dependencies with mocks to isolate unit tests
        repository.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        repository.insertVisit = insertVisit;
    }

    @Test
    void findByPetId_existingPetId_returnsVisitsWithPetSet() {
        // Arrange
        Integer petId = 1;
        Pet pet = new Pet();
        pet.setId(petId);
        pet.setName("Buddy");
        pet.setBirthDate(LocalDate.of(2020, 1, 1));
        PetType petType = new PetType();
        petType.setId(2);
        pet.setType(petType);
        Owner owner = new Owner();
        owner.setId(3);
        pet.setOwner(owner);

        Visit visit1 = new Visit();
        visit1.setId(10);
        visit1.setDate(LocalDate.of(2023, 6, 1));
        visit1.setDescription("Checkup");

        Visit visit2 = new Visit();
        visit2.setId(11);
        visit2.setDate(LocalDate.of(2023, 6, 2));
        visit2.setDescription("Vaccination");

        List<Visit> visits = Arrays.asList(visit1, visit2);

        when(namedParameterJdbcTemplate.queryForObject(
                eq("SELECT id as pets_id, name, birth_date, type_id, owner_id FROM pets WHERE id=:id"),
                any(Map.class),
                any(JdbcPetRowMapper.class)))
            .thenReturn(new JdbcPet() {
                @Override
                public Integer getId() { return pet.getId(); }
                @Override
                public String getName() { return pet.getName(); }
                @Override
                public java.sql.Date getBirthDate() { return Date.valueOf(pet.getBirthDate()); }
                @Override
                public Integer getTypeId() { return pet.getType().getId(); }
                @Override
                public Integer getOwnerId() { return pet.getOwner().getId(); }
            });

        when(namedParameterJdbcTemplate.query(
                eq("SELECT id as visit_id, visit_date, description FROM visits WHERE pet_id=:id"),
                any(Map.class),
                any(JdbcVisitRowMapper.class)))
            .thenReturn(visits);

        // Act
        List<Visit> result = repository.findByPetId(petId);

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).allMatch(v -> v.getPet() != null);
        assertThat(result.get(0).getId()).isEqualTo(10);
        assertThat(result.get(1).getId()).isEqualTo(11);
        assertThat(result.get(0).getDescription()).isEqualTo("Checkup");
        assertThat(result.get(1).getDescription()).isEqualTo("Vaccination");
    }

    @Test
    void findByPetId_nonExistingPetId_throwsEmptyResultDataAccessException() {
        // Arrange
        Integer petId = 999;
        when(namedParameterJdbcTemplate.queryForObject(
                anyString(),
                any(Map.class),
                any(JdbcPetRowMapper.class)))
            .thenThrow(new EmptyResultDataAccessException(1));

        // Act / Assert
        assertThatThrownBy(() -> repository.findByPetId(petId))
            .isInstanceOf(EmptyResultDataAccessException.class);
    }

    @Test
    void findById_existingId_returnsVisit() {
        // Arrange
        int visitId = 5;
        Visit visit = new Visit();
        visit.setId(visitId);
        visit.setDate(LocalDate.of(2023, 5, 20));
        visit.setDescription("Dental cleaning");
        Pet pet = new Pet();
        pet.setId(1);
        visit.setPet(pet);

        when(namedParameterJdbcTemplate.queryForObject(
                anyString(),
                any(Map.class),
                any(JdbcVisitRepositoryImpl.JdbcVisitRowMapperExt.class)))
            .thenReturn(visit);

        // Act
        Visit result = repository.findById(visitId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(visitId);
        assertThat(result.getDescription()).isEqualTo("Dental cleaning");
        assertThat(result.getDate()).isEqualTo(LocalDate.of(2023, 5, 20));
        assertThat(result.getPet()).isNotNull();
        assertThat(result.getPet().getId()).isEqualTo(1);
    }

    @Test
    void findById_nonExistingId_throwsObjectRetrievalFailureException() {
        // Arrange
        int visitId = 999;
        when(namedParameterJdbcTemplate.queryForObject(
                anyString(),
                any(Map.class),
                any(JdbcVisitRepositoryImpl.JdbcVisitRowMapperExt.class)))
            .thenThrow(new EmptyResultDataAccessException(1));

        // Act / Assert
        assertThatThrownBy(() -> repository.findById(visitId))
            .isInstanceOf(ObjectRetrievalFailureException.class)
            .hasMessageContaining(String.valueOf(visitId));
    }

    @Test
    void findAll_returnsAllVisits() {
        // Arrange
        Visit visit1 = new Visit();
        visit1.setId(1);
        visit1.setDate(LocalDate.of(2023, 1, 1));
        visit1.setDescription("Visit 1");
        Pet pet1 = new Pet();
        pet1.setId(1);
        visit1.setPet(pet1);

        Visit visit2 = new Visit();
        visit2.setId(2);
        visit2.setDate(LocalDate.of(2023, 2, 2));
        visit2.setDescription("Visit 2");
        Pet pet2 = new Pet();
        pet2.setId(2);
        visit2.setPet(pet2);

        List<Visit> visits = Arrays.asList(visit1, visit2);

        when(namedParameterJdbcTemplate.query(
                anyString(),
                any(Map.class),
                any(JdbcVisitRepositoryImpl.JdbcVisitRowMapperExt.class)))
            .thenReturn(visits);

        // Act
        var result = repository.findAll();

        // Assert
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(visits);
    }

    @Test
    void save_newVisit_insertsAndSetsId() {
        // Arrange
        Visit visit = new Visit();
        visit.setDate(LocalDate.of(2023, 6, 10));
        visit.setDescription("New visit");
        Pet pet = new Pet();
        pet.setId(1);
        visit.setPet(pet);

        Number generatedKey = 123;
        when(insertVisit.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(generatedKey);

        // Act
        repository.save(visit);

        // Assert
        assertThat(visit.getId()).isEqualTo(generatedKey.intValue());
        verify(insertVisit, times(1)).executeAndReturnKey(any(MapSqlParameterSource.class));
    }

    @Test
    void save_existingVisit_updatesVisit() {
        // Arrange
        Visit visit = new Visit();
        visit.setId(10);
        visit.setDate(LocalDate.of(2023, 6, 11));
        visit.setDescription("Updated visit");
        Pet pet = new Pet();
        pet.setId(2);
        visit.setPet(pet);

        when(namedParameterJdbcTemplate.update(
                anyString(),
                any(MapSqlParameterSource.class)))
            .thenReturn(1);

        // Act
        repository.save(visit);

        // Assert
        verify(namedParameterJdbcTemplate, times(1)).update(
                eq("UPDATE visits SET visit_date=:visit_date, description=:description, pet_id=:pet_id WHERE id=:id "),
                any(MapSqlParameterSource.class));
    }

    @Test
    void delete_existingVisit_deletesVisit() {
        // Arrange
        Visit visit = new Visit();
        visit.setId(20);

        when(namedParameterJdbcTemplate.update(
                anyString(),
                any(Map.class)))
            .thenReturn(1);

        // Act
        repository.delete(visit);

        // Assert
        verify(namedParameterJdbcTemplate, times(1)).update(
                eq("DELETE FROM visits WHERE id=:id"),
                eq(Collections.singletonMap("id", visit.getId())));
    }

}
