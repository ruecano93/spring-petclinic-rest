package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import jakarta.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.repository.OwnerRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class JdbcPetRepositoryImplTest {

	@Mock
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Mock
	private SimpleJdbcInsert insertPet;

	@Mock
	private OwnerRepository ownerRepository;

	private JdbcPetRepositoryImpl jdbcPetRepository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		// We inject mocks directly to avoid DataSource related issues
		jdbcPetRepository = new JdbcPetRepositoryImpl(mock(DataSource.class), ownerRepository);
		// Override the real jdbcTemplate and insertPet with mocks
		jdbcPetRepository.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
		jdbcPetRepository.insertPet = insertPet;
	}

	@Test
	void findPetTypes_whenCalled_shouldReturnListOfPetTypes() {
		// Arrange
		List<PetType> expectedPetTypes = Arrays.asList(new PetType(), new PetType());
		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM types ORDER BY name"), any(Map.class),
				any(BeanPropertyRowMapper.class))).thenReturn(expectedPetTypes);

		// Act
		List<PetType> actualPetTypes = jdbcPetRepository.findPetTypes();

		// Assert
		assertThat(actualPetTypes).isEqualTo(expectedPetTypes);
	}

	@Test
	void findById_whenPetExists_shouldReturnPet() {
		// Arrange
		int petId = 1;
		int ownerId = 2;
		Map<String, Object> params = new HashMap<>();
		params.put("id", petId);

		when(namedParameterJdbcTemplate.queryForObject(eq("SELECT owner_id FROM pets WHERE id=:id"), eq(params),
				eq(Integer.class))).thenReturn(ownerId);

		Owner owner = new Owner();
		Pet pet = new Pet();
		pet.setId(petId);
		owner.getPets().add(pet);

		when(ownerRepository.findById(ownerId)).thenReturn(owner);

		// Act
		Pet actualPet = jdbcPetRepository.findById(petId);

		// Assert
		assertThat(actualPet).isEqualTo(pet);
	}

	@Test
	void findById_whenPetDoesNotExist_shouldThrowObjectRetrievalFailureException() {
		// Arrange
		int petId = 1;
		Map<String, Object> params = new HashMap<>();
		params.put("id", petId);

		when(namedParameterJdbcTemplate.queryForObject(eq("SELECT owner_id FROM pets WHERE id=:id"), eq(params),
				eq(Integer.class))).thenThrow(new EmptyResultDataAccessException(1));

		// Act & Assert
		assertThatThrownBy(() -> jdbcPetRepository.findById(petId))
				.isInstanceOf(ObjectRetrievalFailureException.class)
				.hasMessageContaining(String.valueOf(petId));
	}

	@Test
	void save_whenPetIsNew_shouldInsertPetAndSetId() {
		// Arrange
		Pet pet = new Pet();
		pet.setId(0);
		pet.setName("Buddy");
		pet.setBirthDate(LocalDate.of(2020, 1, 1));
		PetType petType = new PetType();
		petType.setId(5);
		pet.setType(petType);
		Owner owner = new Owner();
		owner.setId(10);
		pet.setOwner(owner);

		// Pet is new
		Pet spyPet = org.mockito.Mockito.spy(pet);
		when(spyPet.isNew()).thenReturn(true);

		when(insertPet.executeAndReturnKey(any(MapSqlParameterSource.class))).thenReturn(123);

		// Act
		jdbcPetRepository.save(spyPet);

		// Assert
		assertThat(spyPet.getId()).isEqualTo(123);
		verify(insertPet).executeAndReturnKey(any(MapSqlParameterSource.class));
	}

	@Test
	void save_whenPetExists_shouldUpdatePet() {
		// Arrange
		Pet pet = new Pet();
		pet.setId(1);
		pet.setName("Buddy");
		pet.setBirthDate(LocalDate.of(2020, 1, 1));
		PetType petType = new PetType();
		petType.setId(5);
		pet.setType(petType);
		Owner owner = new Owner();
		owner.setId(10);
		pet.setOwner(owner);

		// Pet is not new
		Pet spyPet = org.mockito.Mockito.spy(pet);
		when(spyPet.isNew()).thenReturn(false);

		when(namedParameterJdbcTemplate.update(any(String.class), any(MapSqlParameterSource.class))).thenReturn(1);

		// Act
		jdbcPetRepository.save(spyPet);

		// Assert
		verify(namedParameterJdbcTemplate).update(eq("UPDATE pets SET name=:name, birth_date=:birth_date, type_id=:type_id, " +
				"owner_id=:owner_id WHERE id=:id"), any(MapSqlParameterSource.class));
	}

	@Test
	void findAll_whenCalled_shouldReturnAllPetsWithTypesAndOwners() {
		// Arrange
		JdbcPet jdbcPet1 = new JdbcPet();
		jdbcPet1.setId(1);
		jdbcPet1.setName("Buddy");
		jdbcPet1.setBirthDate(LocalDate.of(2020, 1, 1));
		jdbcPet1.setTypeId(1);
		jdbcPet1.setOwnerId(1);

		List<JdbcPet> jdbcPets = Collections.singletonList(jdbcPet1);

		PetType petType = new PetType();
		petType.setId(1);
		petType.setName("Dog");
		List<PetType> petTypes = Collections.singletonList(petType);

		Owner owner = new Owner();
		owner.setId(1);
		owner.setFirstName("John");
		owner.setLastName("Doe");
		List<Owner> owners = Collections.singletonList(owner);

		when(namedParameterJdbcTemplate.query(eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id FROM pets"),
				any(Map.class), any(JdbcPetRowMapper.class))).thenReturn(jdbcPets);

		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM types ORDER BY name"), any(Map.class),
				any(BeanPropertyRowMapper.class))).thenReturn(petTypes);

		when(namedParameterJdbcTemplate.query(eq("SELECT id, first_name, last_name, address, city, telephone FROM owners ORDER BY last_name"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(owners);

		// Act
		Collection<Pet> pets = jdbcPetRepository.findAll();

		// Assert
		assertThat(pets).hasSize(1);
		Pet pet = pets.iterator().next();
		assertThat(pet.getId()).isEqualTo(1);
		assertThat(pet.getType()).isEqualTo(petType);
		assertThat(pet.getOwner()).isEqualTo(owner);
	}

	@Test
	void findAllPageable_whenCalled_shouldReturnPageOfPets() {
		// Arrange
		int page = 0;
		int size = 2;
		Pageable pageable = PageRequest.of(page, size);

		JdbcPet jdbcPet1 = new JdbcPet();
		jdbcPet1.setId(1);
		jdbcPet1.setName("Buddy");
		jdbcPet1.setBirthDate(LocalDate.of(2020, 1, 1));
		jdbcPet1.setTypeId(1);
		jdbcPet1.setOwnerId(1);

		JdbcPet jdbcPet2 = new JdbcPet();
		jdbcPet2.setId(2);
		jdbcPet2.setName("Kitty");
		jdbcPet2.setBirthDate(LocalDate.of(2021, 2, 2));
		jdbcPet2.setTypeId(2);
		jdbcPet2.setOwnerId(2);

		List<JdbcPet> jdbcPets = Arrays.asList(jdbcPet1, jdbcPet2);

		PetType petType1 = new PetType();
		petType1.setId(1);
		petType1.setName("Dog");
		PetType petType2 = new PetType();
		petType2.setId(2);
		petType2.setName("Cat");
		List<PetType> petTypes = Arrays.asList(petType1, petType2);

		Owner owner1 = new Owner();
		owner1.setId(1);
		owner1.setFirstName("John");
		owner1.setLastName("Doe");
		Owner owner2 = new Owner();
		owner2.setId(2);
		owner2.setFirstName("Jane");
		owner2.setLastName("Smith");
		List<Owner> owners = Arrays.asList(owner1, owner2);

		Map<String, Object> params = new HashMap<>();
		params.put("size", size);
		params.put("offset", (long) page * size);

		when(namedParameterJdbcTemplate.query(eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id FROM pets ORDER BY id LIMIT :size OFFSET :offset"),
				eq(params), any(JdbcPetRowMapper.class))).thenReturn(jdbcPets);

		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM types ORDER BY name"), any(Map.class),
				any(BeanPropertyRowMapper.class))).thenReturn(petTypes);

		when(namedParameterJdbcTemplate.query(eq("SELECT id, first_name, last_name, address, city, telephone FROM owners ORDER BY last_name"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(owners);

		when(namedParameterJdbcTemplate.queryForObject(eq("SELECT count(*) FROM pets"), eq(params), eq(Long.class))).thenReturn(2L);

		// Act
		Page<Pet> pageResult = jdbcPetRepository.findAll(pageable);

		// Assert
		assertThat(pageResult.getTotalElements()).isEqualTo(2);
		assertThat(pageResult.getContent()).hasSize(2);
		Pet pet1 = pageResult.getContent().get(0);
		assertThat(pet1.getType()).isEqualTo(petType1);
		assertThat(pet1.getOwner()).isEqualTo(owner1);
		Pet pet2 = pageResult.getContent().get(1);
		assertThat(pet2.getType()).isEqualTo(petType2);
		assertThat(pet2.getOwner()).isEqualTo(owner2);
	}

	@Test
	void delete_whenCalled_shouldDeleteVisitsAndPet() {
		// Arrange
		Pet pet = new Pet();
		pet.setId(1);

		Visit visit1 = new Visit();
		visit1.setId(10);
		Visit visit2 = new Visit();
		visit2.setId(20);

		pet.getVisits().add(visit1);
		pet.getVisits().add(visit2);

		when(namedParameterJdbcTemplate.update(eq("DELETE FROM visits WHERE id=:id"), any(Map.class))).thenReturn(1);
		when(namedParameterJdbcTemplate.update(eq("DELETE FROM pets WHERE id=:id"), any(Map.class))).thenReturn(1);

		// Act
		jdbcPetRepository.delete(pet);

		// Assert
		ArgumentCaptor<Map<String, Object>> visitCaptor = ArgumentCaptor.forClass(Map.class);
		verify(namedParameterJdbcTemplate).update(eq("DELETE FROM visits WHERE id=:id"), visitCaptor.capture());
		Map<String, Object> capturedVisitParam = visitCaptor.getValue();
		assertThat(capturedVisitParam).containsKey("id");

		verify(namedParameterJdbcTemplate).update(eq("DELETE FROM pets WHERE id=:id"), any(Map.class));
	}

}
