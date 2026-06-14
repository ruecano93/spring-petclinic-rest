package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

class JdbcOwnerRepositoryImplTest {

    @Mock
    NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    @Mock
    SimpleJdbcInsert insertOwner;

    @Mock
    DataSource dataSource;

    @InjectMocks
    JdbcOwnerRepositoryImpl repository;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        repository = new JdbcOwnerRepositoryImpl(dataSource);
        repository.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
        repository.insertOwner = insertOwner;
    }

    @Test
    void findByLastName_validLastName_returnsOwnersWithPetsAndVisits() {
        // Arrange
        String lastName = "Smith";
        Owner owner = new Owner();
        owner.setId(1);
        owner.setLastName("Smith");
        List<Owner> owners = new ArrayList<>();
        owners.add(owner);

        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<JdbcPet> pets = new ArrayList<>();
        JdbcPet pet = new JdbcPet();
        pet.setId(1);
        pet.setName("Buddy");
        pet.setTypeId(1);
        pets.add(pet);

        // Mock query for owners
        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like :lastName"),
            any(Map.class),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenReturn(owners);

        // Mock query for pet types
        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        // Mock query for pets and visits
        when(namedParameterJdbcTemplate.query(
            eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id, visits.id as visit_id, visit_date, description, visits.pet_id as visits_pet_id FROM pets LEFT OUTER JOIN visits ON pets.id = visits.pet_id WHERE owner_id=:id ORDER BY pets.id"),
            any(Map.class),
            any(JdbcPetVisitExtractor.class)
        )).thenReturn(pets);

        // Act
        var result = repository.findByLastName(lastName);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        Owner resultOwner = result.iterator().next();
        assertThat(resultOwner.getPets()).hasSize(1);
        Pet resultPet = resultOwner.getPets().get(0);
        assertThat(resultPet.getName()).isEqualTo("Buddy");
        assertThat(resultPet.getType()).isEqualTo(petType);
    }

    @Test
    void findByLastName_withPageable_validLastName_returnsPagedOwnersWithPetsAndVisits() {
        // Arrange
        String lastName = "Smith";
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        Owner owner = new Owner();
        owner.setId(1);
        owner.setLastName("Smith");
        List<Owner> owners = new ArrayList<>();
        owners.add(owner);

        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<JdbcPet> pets = new ArrayList<>();
        JdbcPet pet = new JdbcPet();
        pet.setId(1);
        pet.setName("Buddy");
        pet.setTypeId(1);
        pets.add(pet);

        Map<String, Object> params = new HashMap<>();
        params.put("lastName", lastName + "%");
        params.put("size", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        // Mock query for owners with pagination
        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE last_name like :lastName ORDER BY id LIMIT :size OFFSET :offset"),
            eq(params),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenReturn(owners);

        // Mock query for total count
        when(namedParameterJdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM owners WHERE last_name like :lastName"),
            eq(params),
            eq(Long.class)
        )).thenReturn(1L);

        // Mock query for pet types
        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        // Mock query for pets and visits
        when(namedParameterJdbcTemplate.query(
            eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id, visits.id as visit_id, visit_date, description, visits.pet_id as visits_pet_id FROM pets LEFT OUTER JOIN visits ON pets.id = visits.pet_id WHERE owner_id=:id ORDER BY pets.id"),
            any(Map.class),
            any(JdbcPetVisitExtractor.class)
        )).thenReturn(pets);

        // Act
        Page<Owner> result = repository.findByLastName(lastName, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        Owner resultOwner = result.getContent().get(0);
        assertThat(resultOwner.getPets()).hasSize(1);
        Pet resultPet = resultOwner.getPets().get(0);
        assertThat(resultPet.getName()).isEqualTo("Buddy");
        assertThat(resultPet.getType()).isEqualTo(petType);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void findById_existingId_returnsOwnerWithPetsAndVisits() {
        // Arrange
        int id = 1;
        Owner owner = new Owner();
        owner.setId(id);
        owner.setLastName("Smith");

        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<JdbcPet> pets = new ArrayList<>();
        JdbcPet pet = new JdbcPet();
        pet.setId(1);
        pet.setName("Buddy");
        pet.setTypeId(1);
        pets.add(pet);

        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        // Mock queryForObject for owner
        when(namedParameterJdbcTemplate.queryForObject(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE id= :id"),
            eq(params),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenReturn(owner);

        // Mock query for pet types
        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        // Mock query for pets and visits
        when(namedParameterJdbcTemplate.query(
            eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id, visits.id as visit_id, visit_date, description, visits.pet_id as visits_pet_id FROM pets LEFT OUTER JOIN visits ON pets.id = visits.pet_id WHERE owner_id=:id ORDER BY pets.id"),
            eq(params),
            any(JdbcPetVisitExtractor.class)
        )).thenReturn(pets);

        // Act
        Owner result = repository.findById(id);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(id);
        assertThat(result.getPets()).hasSize(1);
        Pet resultPet = result.getPets().get(0);
        assertThat(resultPet.getName()).isEqualTo("Buddy");
        assertThat(resultPet.getType()).isEqualTo(petType);
    }

    @Test
    void findById_nonExistingId_throwsObjectRetrievalFailureException() {
        // Arrange
        int id = 999;
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);

        when(namedParameterJdbcTemplate.queryForObject(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners WHERE id= :id"),
            eq(params),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenThrow(new EmptyResultDataAccessException(1));

        // Act & Assert
        assertThatCode(() -> repository.findById(id))
            .isInstanceOf(ObjectRetrievalFailureException.class)
            .hasMessageContaining(String.valueOf(id));
    }

    @Test
    void save_newOwner_insertsOwnerAndSetsId() {
        // Arrange
        Owner owner = new Owner();
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setAddress("123 Street");
        owner.setCity("City");
        owner.setTelephone("123456789");
        // isNew returns true if id == 0
        owner.setId(0);

        when(insertOwner.executeAndReturnKey(any(BeanPropertySqlParameterSource.class))).thenReturn(42);

        // Act
        repository.save(owner);

        // Assert
        assertThat(owner.getId()).isEqualTo(42);
    }

    @Test
    void save_existingOwner_updatesOwner() {
        // Arrange
        Owner owner = new Owner();
        owner.setId(1);
        owner.setFirstName("John");
        owner.setLastName("Doe");
        owner.setAddress("123 Street");
        owner.setCity("City");
        owner.setTelephone("123456789");

        when(namedParameterJdbcTemplate.update(
            anyString(),
            any(BeanPropertySqlParameterSource.class)
        )).thenReturn(1);

        // Act
        repository.save(owner);

        // Assert
        verify(namedParameterJdbcTemplate, times(1)).update(
            eq("UPDATE owners SET first_name=:firstName, last_name=:lastName, address=:address, city=:city, telephone=:telephone WHERE id=:id"),
            any(BeanPropertySqlParameterSource.class)
        );
    }

    @Test
    void getPetTypes_returnsListOfPetTypes() {
        // Arrange
        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        // Act
        Collection<PetType> result = repository.getPetTypes();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.iterator().next().getName()).isEqualTo("Dog");
    }

    @Test
    void findAll_returnsAllOwnersWithPetsAndVisits() {
        // Arrange
        Owner owner = new Owner();
        owner.setId(1);
        owner.setLastName("Smith");
        List<Owner> owners = new ArrayList<>();
        owners.add(owner);

        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<JdbcPet> pets = new ArrayList<>();
        JdbcPet pet = new JdbcPet();
        pet.setId(1);
        pet.setName("Buddy");
        pet.setTypeId(1);
        pets.add(pet);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenReturn(owners);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id, visits.id as visit_id, visit_date, description, visits.pet_id as visits_pet_id FROM pets LEFT OUTER JOIN visits ON pets.id = visits.pet_id WHERE owner_id=:id ORDER BY pets.id"),
            any(Map.class),
            any(JdbcPetVisitExtractor.class)
        )).thenReturn(pets);

        // Act
        Collection<Owner> result = repository.findAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        Owner resultOwner = result.iterator().next();
        assertThat(resultOwner.getPets()).hasSize(1);
        Pet resultPet = resultOwner.getPets().get(0);
        assertThat(resultPet.getName()).isEqualTo("Buddy");
        assertThat(resultPet.getType()).isEqualTo(petType);
    }

    @Test
    void findAll_withPageable_returnsPagedOwnersWithPetsAndVisits() {
        // Arrange
        Pageable pageable = Pageable.ofSize(10).withPage(0);
        Owner owner = new Owner();
        owner.setId(1);
        owner.setLastName("Smith");
        List<Owner> owners = new ArrayList<>();
        owners.add(owner);

        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<JdbcPet> pets = new ArrayList<>();
        JdbcPet pet = new JdbcPet();
        pet.setId(1);
        pet.setName("Buddy");
        pet.setTypeId(1);
        pets.add(pet);

        Map<String, Object> params = new HashMap<>();
        params.put("size", pageable.getPageSize());
        params.put("offset", pageable.getOffset());

        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, first_name, last_name, address, city, telephone FROM owners ORDER BY id LIMIT :size OFFSET :offset"),
            eq(params),
            eq(BeanPropertyRowMapper.newInstance(Owner.class))
        )).thenReturn(owners);

        when(namedParameterJdbcTemplate.queryForObject(
            eq("SELECT COUNT(*) FROM owners"),
            eq(params),
            eq(Long.class)
        )).thenReturn(1L);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT id, name FROM types ORDER BY name"),
            eq(new HashMap<>()),
            eq(BeanPropertyRowMapper.newInstance(PetType.class))
        )).thenReturn(petTypes);

        when(namedParameterJdbcTemplate.query(
            eq("SELECT pets.id as pets_id, name, birth_date, type_id, owner_id, visits.id as visit_id, visit_date, description, visits.pet_id as visits_pet_id FROM pets LEFT OUTER JOIN visits ON pets.id = visits.pet_id WHERE owner_id=:id ORDER BY pets.id"),
            any(Map.class),
            any(JdbcPetVisitExtractor.class)
        )).thenReturn(pets);

        // Act
        Page<Owner> result = repository.findAll(pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        Owner resultOwner = result.getContent().get(0);
        assertThat(resultOwner.getPets()).hasSize(1);
        Pet resultPet = resultOwner.getPets().get(0);
        assertThat(resultPet.getName()).isEqualTo("Buddy");
        assertThat(resultPet.getType()).isEqualTo(petType);
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void delete_ownerWithPetsAndVisits_deletesAllCascade() {
        // Arrange
        Owner owner = new Owner();
        owner.setId(1);

        Pet pet = new Pet();
        pet.setId(10);
        Visit visit = new Visit();
        visit.setId(100);
        pet.setVisits(Collections.singletonList(visit));
        owner.setPets(Collections.singletonList(pet));

        Map<String, Object> ownerParams = new HashMap<>();
        ownerParams.put("id", owner.getId());

        Map<String, Object> petParams = new HashMap<>();
        petParams.put("id", pet.getId());

        Map<String, Object> visitParams = new HashMap<>();
        visitParams.put("id", visit.getId());

        when(namedParameterJdbcTemplate.update(eq("DELETE FROM visits WHERE id=:id"), eq(visitParams))).thenReturn(1);
        when(namedParameterJdbcTemplate.update(eq("DELETE FROM pets WHERE id=:id"), eq(petParams))).thenReturn(1);
        when(namedParameterJdbcTemplate.update(eq("DELETE FROM owners WHERE id=:id"), eq(ownerParams))).thenReturn(1);

        // Act
        repository.delete(owner);

        // Assert
        verify(namedParameterJdbcTemplate, times(1)).update(eq("DELETE FROM visits WHERE id=:id"), eq(visitParams));
        verify(namedParameterJdbcTemplate, times(1)).update(eq("DELETE FROM pets WHERE id=:id"), eq(petParams));
        verify(namedParameterJdbcTemplate, times(1)).update(eq("DELETE FROM owners WHERE id=:id"), eq(ownerParams));
    }
}
