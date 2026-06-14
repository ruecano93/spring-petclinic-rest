package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;

class JdbcPetTypeRepositoryImplTest {

	@Mock
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Mock
	SimpleJdbcInsert insertPetType;

	JdbcPetTypeRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		repository = new JdbcPetTypeRepositoryImpl(null);
		repository.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		repository.insertPetType = insertPetType;
	}

	@Test
	void findById_existingId_returnsPetType() {
		// Arrange
		int id = 1;
		PetType expectedPetType = new PetType();
		expectedPetType.setId(id);
		expectedPetType.setName("dog");
		when(namedParameterJdbcTemplate.queryForObject(
				eq("SELECT id, name FROM types WHERE id= :id"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(expectedPetType);

		// Act
		PetType actualPetType = repository.findById(id);

		// Assert
		assertThat(actualPetType).isEqualTo(expectedPetType);
	}

	@Test
	void findById_nonExistingId_throwsObjectRetrievalFailureException() {
		// Arrange
		int id = 99;
		when(namedParameterJdbcTemplate.queryForObject(
				eq("SELECT id, name FROM types WHERE id= :id"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenThrow(new EmptyResultDataAccessException(1));

		// Act / Assert
		assertThatCode(() -> repository.findById(id))
			.isInstanceOf(ObjectRetrievalFailureException.class)
			.hasMessageContaining(String.valueOf(id));
	}

	@Test
	void findByName_existingName_returnsPetType() {
		// Arrange
		String name = "dog";
		PetType expectedPetType = new PetType();
		expectedPetType.setId(1);
		expectedPetType.setName(name);
		when(namedParameterJdbcTemplate.queryForObject(
				eq("SELECT id, name FROM types WHERE name= :name"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(expectedPetType);

		// Act
		PetType actualPetType = repository.findByName(name);

		// Assert
		assertThat(actualPetType).isEqualTo(expectedPetType);
	}

	@Test
	void findByName_nonExistingName_throwsObjectRetrievalFailureException() {
		// Arrange
		String name = "nonexistent";
		when(namedParameterJdbcTemplate.queryForObject(
				eq("SELECT id, name FROM types WHERE name= :name"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenThrow(new EmptyResultDataAccessException(1));

		// Act / Assert
		assertThatCode(() -> repository.findByName(name))
			.isInstanceOf(ObjectRetrievalFailureException.class)
			.hasMessageContaining(name);
	}

	@Test
	void findAll_returnsListOfPetTypes() {
		// Arrange
		PetType pt1 = new PetType();
		pt1.setId(1);
		pt1.setName("dog");
		PetType pt2 = new PetType();
		pt2.setId(2);
		pt2.setName("cat");
		List<PetType> expectedList = List.of(pt1, pt2);
		when(namedParameterJdbcTemplate.query(
				eq("SELECT id, name FROM types"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(expectedList);

		// Act
		var actualList = repository.findAll();

		// Assert
		assertThat(actualList).containsExactlyElementsOf(expectedList);
	}

	@Test
	void save_newPetType_insertsAndSetsId() {
		// Arrange
		PetType petType = new PetType();
		petType.setName("hamster");
		// isNew returns true if id == 0
		assertThat(petType.isNew()).isTrue();

		when(insertPetType.executeAndReturnKey(any(BeanPropertySqlParameterSource.class)))
			.thenReturn(42);

		// Act
		repository.save(petType);

		// Assert
		verify(insertPetType).executeAndReturnKey(any(BeanPropertySqlParameterSource.class));
		assertThat(petType.getId()).isEqualTo(42);
	}

	@Test
	void save_existingPetType_updatesRecord() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(10);
		petType.setName("bird");
		assertThat(petType.isNew()).isFalse();

		// Act
		repository.save(petType);

		// Assert
		ArgumentCaptor<BeanPropertySqlParameterSource> captor = ArgumentCaptor.forClass(BeanPropertySqlParameterSource.class);
		verify(namedParameterJdbcTemplate).update(eq("UPDATE types SET name=:name WHERE id=:id"), captor.capture());
		BeanPropertySqlParameterSource paramSource = captor.getValue();
		assertThat(paramSource.getValue("id")).isEqualTo(10);
		assertThat(paramSource.getValue("name")).isEqualTo("bird");
	}

	@Test
	void delete_petTypeWithPetsAndVisits_deletesCascade() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(1);

		Pet pet1 = new Pet();
		pet1.setId(11);
		Pet pet2 = new Pet();
		pet2.setId(12);

		Visit visit1 = new Visit();
		visit1.setId(101);
		Visit visit2 = new Visit();
		visit2.setId(102);

		when(namedParameterJdbcTemplate.query(
				eq("SELECT pets.id, name, birth_date, type_id, owner_id FROM pets WHERE type_id=:id"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(List.of(pet1, pet2));

		when(namedParameterJdbcTemplate.query(
				eq("SELECT id, pet_id, visit_date, description FROM visits WHERE pet_id = :id"),
				argThat(map -> map.containsValue(11)),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(List.of(visit1));

		when(namedParameterJdbcTemplate.query(
				eq("SELECT id, pet_id, visit_date, description FROM visits WHERE pet_id = :id"),
				argThat(map -> map.containsValue(12)),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(List.of(visit2));

		// Act
		repository.delete(petType);

		// Assert
		// Visits deleted first
		verify(namedParameterJdbcTemplate).update("DELETE FROM visits WHERE id=:id", Map.of("id", 101));
		verify(namedParameterJdbcTemplate).update("DELETE FROM visits WHERE id=:id", Map.of("id", 102));
		// Pets deleted next
		verify(namedParameterJdbcTemplate).update("DELETE FROM pets WHERE id=:id", Map.of("id", 11));
		verify(namedParameterJdbcTemplate).update("DELETE FROM pets WHERE id=:id", Map.of("id", 12));
		// PetType deleted last
		verify(namedParameterJdbcTemplate).update("DELETE FROM types WHERE id=:id", Map.of("id", 1));
	}

	@Test
	void delete_petTypeWithoutPets_deletesOnlyPetType() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(2);

		when(namedParameterJdbcTemplate.query(
				eq("SELECT pets.id, name, birth_date, type_id, owner_id FROM pets WHERE type_id=:id"),
				any(Map.class),
				any(BeanPropertyRowMapper.class)))
			.thenReturn(List.of());

		// Act
		repository.delete(petType);

		// Assert
		verify(namedParameterJdbcTemplate).update("DELETE FROM types WHERE id=:id", Map.of("id", 2));
		verify(namedParameterJdbcTemplate).query(
				eq("SELECT pets.id, name, birth_date, type_id, owner_id FROM pets WHERE type_id=:id"),
				any(Map.class),
				any(BeanPropertyRowMapper.class));
	}

}
