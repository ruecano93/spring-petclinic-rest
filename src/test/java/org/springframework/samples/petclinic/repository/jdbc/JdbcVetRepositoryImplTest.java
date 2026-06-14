package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.Specialty;
import org.springframework.samples.petclinic.model.Vet;

class JdbcVetRepositoryImplTest {

	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
	private SimpleJdbcInsert insertVet;
	private JdbcVetRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		dataSource = mock(DataSource.class);
		jdbcTemplate = mock(JdbcTemplate.class);
		namedParameterJdbcTemplate = mock(NamedParameterJdbcTemplate.class);
		insertVet = mock(SimpleJdbcInsert.class);
		// We cannot inject mocks into constructor directly for insertVet and namedParameterJdbcTemplate
		// So we create repository with real constructor and then override fields by reflection or subclass
		// But here we create a subclass to inject mocks for test
		repository = new JdbcVetRepositoryImpl(dataSource, jdbcTemplate) {
			{
				this.namedParameterJdbcTemplate = JdbcVetRepositoryImplTest.this.namedParameterJdbcTemplate;
				this.insertVet = JdbcVetRepositoryImplTest.this.insertVet;
			}
		};
	}

	@Test
	void findAll_whenCalled_shouldReturnListOfVetsWithSpecialties() {
		// Arrange
		Vet vet1 = new Vet();
		vet1.setId(1);
		vet1.setFirstName("John");
		vet1.setLastName("Doe");
		Vet vet2 = new Vet();
		vet2.setId(2);
		vet2.setFirstName("Jane");
		vet2.setLastName("Smith");
		List<Vet> vetsFromDb = List.of(vet1, vet2);

		Specialty spec1 = new Specialty();
		spec1.setId(10);
		spec1.setName("Surgery");
		Specialty spec2 = new Specialty();
		spec2.setId(20);
		spec2.setName("Dentistry");
		List<Specialty> specialtiesFromDb = List.of(spec1, spec2);

		when(jdbcTemplate.query(eq("SELECT id, first_name, last_name FROM vets ORDER BY last_name,first_name"),
				any(BeanPropertyRowMapper.class))).thenReturn(vetsFromDb);

		when(jdbcTemplate.query(eq("SELECT id, name FROM specialties"),
				any(BeanPropertyRowMapper.class))).thenReturn(specialtiesFromDb);

		when(jdbcTemplate.query(eq("SELECT specialty_id FROM vet_specialties WHERE vet_id=?"),
				any(BeanPropertyRowMapper.class), eq(1))).thenReturn(List.of(10));
		when(jdbcTemplate.query(eq("SELECT specialty_id FROM vet_specialties WHERE vet_id=?"),
				any(BeanPropertyRowMapper.class), eq(2))).thenReturn(List.of(20));

		// Act
		Collection<Vet> result = repository.findAll();

		// Assert
		assertThat(result).hasSize(2);
		for (Vet vet : result) {
			if (vet.getId() == 1) {
				assertThat(vet.getSpecialties()).hasSize(1);
				assertThat(vet.getSpecialties()).extracting("id").containsExactly(10);
			} else if (vet.getId() == 2) {
				assertThat(vet.getSpecialties()).hasSize(1);
				assertThat(vet.getSpecialties()).extracting("id").containsExactly(20);
			}
		}
	}

	@Test
	void findById_whenVetExists_shouldReturnVetWithSpecialties() {
		// Arrange
		int vetId = 5;
		Vet vet = new Vet();
		vet.setId(vetId);
		vet.setFirstName("Alice");
		vet.setLastName("Wonder");

		Specialty spec1 = new Specialty();
		spec1.setId(100);
		spec1.setName("Radiology");
		Specialty spec2 = new Specialty();
		spec2.setId(200);
		spec2.setName("Pathology");
		List<Specialty> specialties = List.of(spec1, spec2);

		Map<String, Object> params = new HashMap<>();
		params.put("id", vetId);

		when(namedParameterJdbcTemplate.queryForObject(
				eq("SELECT id, first_name, last_name FROM vets WHERE id= :id"),
				eq(params),
				any(BeanPropertyRowMapper.class))).thenReturn(vet);

		when(namedParameterJdbcTemplate.query(
				eq("SELECT id, name FROM specialties"),
				eq(params),
				any(BeanPropertyRowMapper.class))).thenReturn(specialties);

		when(namedParameterJdbcTemplate.query(
				eq("SELECT specialty_id FROM vet_specialties WHERE vet_id=:id"),
				eq(params),
				any(BeanPropertyRowMapper.class))).thenReturn(List.of(100, 200));

		// Act
		Vet result = repository.findById(vetId);

		// Assert
		assertThat(result).isNotNull();
		assertThat(result.getId()).isEqualTo(vetId);
		assertThat(result.getSpecialties()).hasSize(2);
		assertThat(result.getSpecialties()).extracting("id").containsExactlyInAnyOrder(100, 200);
	}

	@Test
	void findById_whenVetDoesNotExist_shouldThrowObjectRetrievalFailureException() {
		// Arrange
		int vetId = 999;
		Map<String, Object> params = new HashMap<>();
		params.put("id", vetId);

		when(namedParameterJdbcTemplate.queryForObject(
				anyString(),
				eq(params),
				any(BeanPropertyRowMapper.class))).thenThrow(new EmptyResultDataAccessException(1));

		// Act Assert
		assertThatCode(() -> repository.findById(vetId))
			.isInstanceOf(ObjectRetrievalFailureException.class)
			.hasMessageContaining(String.valueOf(vetId));
	}

	@Test
	void save_whenVetIsNew_shouldInsertVetAndUpdateSpecialties() {
		// Arrange
		Vet vet = new Vet();
		vet.setFirstName("New");
		vet.setLastName("Vet");
		vet.setId(0);
		Specialty spec1 = new Specialty();
		spec1.setId(1);
		Specialty spec2 = new Specialty();
		spec2.setId(2);
		vet.addSpecialty(spec1);
		vet.addSpecialty(spec2);

		when(vet.isNew()).thenCallRealMethod();
		when(insertVet.executeAndReturnKey(any(BeanPropertySqlParameterSource.class))).thenReturn(123);

		doNothing().when(namedParameterJdbcTemplate).update(anyString(), anyMap());

		// Act
		repository.save(vet);

		// Assert
		assertThat(vet.getId()).isEqualTo(123);
		verify(insertVet, times(1)).executeAndReturnKey(any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vet_specialties WHERE vet_id=:id", Map.of("id", 123));
		verify(namedParameterJdbcTemplate, times(2)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 123, "spec_id", 1));
		verify(namedParameterJdbcTemplate, times(2)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 123, "spec_id", 2));
	}

	@Test
	void save_whenVetExists_shouldUpdateVetAndSpecialties() {
		// Arrange
		Vet vet = new Vet();
		vet.setId(10);
		vet.setFirstName("Existing");
		vet.setLastName("Vet");
		Specialty spec1 = new Specialty();
		spec1.setId(5);
		vet.addSpecialty(spec1);

		when(vet.isNew()).thenCallRealMethod();

		doNothing().when(namedParameterJdbcTemplate).update(anyString(), any(BeanPropertySqlParameterSource.class));
		doNothing().when(namedParameterJdbcTemplate).update(anyString(), anyMap());

		// Act
		repository.save(vet);

		// Assert
		verify(namedParameterJdbcTemplate, times(1)).update(eq("UPDATE vets SET first_name=:firstName, last_name=:lastName WHERE id=:id"), any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vet_specialties WHERE vet_id=:id", Map.of("id", 10));
		verify(namedParameterJdbcTemplate, times(1)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 10, "spec_id", 5));
	}

	@Test
	void delete_whenCalled_shouldDeleteVetSpecialtiesAndVet() {
		// Arrange
		Vet vet = new Vet();
		vet.setId(77);

		doNothing().when(namedParameterJdbcTemplate).update(anyString(), anyMap());

		// Act
		repository.delete(vet);

		// Assert
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vet_specialties WHERE vet_id=:id", Map.of("id", 77));
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vets WHERE id=:id", Map.of("id", 77));
	}

	@Test
	void updateVetSpecialties_whenVetHasSpecialties_shouldDeleteAndInsertSpecialties() {
		// Arrange
		Vet vet = new Vet();
		vet.setId(50);
		Specialty spec1 = new Specialty();
		spec1.setId(11);
		Specialty spec2 = new Specialty();
		spec2.setId(22);
		vet.addSpecialty(spec1);
		vet.addSpecialty(spec2);

		doNothing().when(namedParameterJdbcTemplate).update(anyString(), anyMap());

		// Act
		repository.save(vet);

		// Assert
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vet_specialties WHERE vet_id=:id", Map.of("id", 50));
		verify(namedParameterJdbcTemplate, times(1)).update("UPDATE vets SET first_name=:firstName, last_name=:lastName WHERE id=:id", new BeanPropertySqlParameterSource(vet));
		verify(namedParameterJdbcTemplate, times(1)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 50, "spec_id", 11));
		verify(namedParameterJdbcTemplate, times(1)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 50, "spec_id", 22));
	}

	@Test
	void updateVetSpecialties_whenVetHasSpecialtyWithNullId_shouldNotInsertThatSpecialty() {
		// Arrange
		Vet vet = new Vet();
		vet.setId(60);
		Specialty spec1 = new Specialty();
		spec1.setId(null);
		Specialty spec2 = new Specialty();
		spec2.setId(33);
		vet.addSpecialty(spec1);
		vet.addSpecialty(spec2);

		doNothing().when(namedParameterJdbcTemplate).update(anyString(), anyMap());

		// Act
		repository.save(vet);

		// Assert
		verify(namedParameterJdbcTemplate, times(1)).update("DELETE FROM vet_specialties WHERE vet_id=:id", Map.of("id", 60));
		verify(namedParameterJdbcTemplate, times(1)).update("UPDATE vets SET first_name=:firstName, last_name=:lastName WHERE id=:id", new BeanPropertySqlParameterSource(vet));
		verify(namedParameterJdbcTemplate, times(0)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 60, "spec_id", null));
		verify(namedParameterJdbcTemplate, times(1)).update("INSERT INTO vet_specialties VALUES (:id, :spec_id)", Map.of("id", 60, "spec_id", 33));
	}

}
