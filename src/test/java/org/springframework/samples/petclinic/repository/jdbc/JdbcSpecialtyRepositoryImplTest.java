package org.springframework.samples.petclinic.repository.jdbc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.*;

import javax.sql.DataSource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.BeanPropertySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.samples.petclinic.model.Specialty;

class JdbcSpecialtyRepositoryImplTest {

	@Mock
	DataSource dataSource;

	@Mock
	NamedParameterJdbcTemplate namedParameterJdbcTemplate;

	@Mock
	SimpleJdbcInsert insertSpecialty;

	JdbcSpecialtyRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		// Create repository instance without calling constructor that creates real jdbcTemplate
		repository = new JdbcSpecialtyRepositoryImpl(dataSource) {
			{
				this.namedParameterJdbcTemplate = namedParameterJdbcTemplate;
				this.insertSpecialty = insertSpecialty;
			}
		};
	}

	@Test
	void findById_existingId_returnsSpecialty() {
		// Arrange
		int id = 1;
		Specialty expected = new Specialty();
		expected.setId(id);
		expected.setName("radiology");
		when(namedParameterJdbcTemplate.queryForObject(eq("SELECT id, name FROM specialties WHERE id= :id"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(expected);

		// Act
		Specialty actual = repository.findById(id);

		// Assert
		assertThat(actual).isNotNull();
		assertThat(actual.getId()).isEqualTo(id);
		assertThat(actual.getName()).isEqualTo("radiology");
	}

	@Test
	void findById_nonExistingId_throwsObjectRetrievalFailureException() {
		// Arrange
		int id = 999;
		when(namedParameterJdbcTemplate.queryForObject(eq("SELECT id, name FROM specialties WHERE id= :id"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenThrow(EmptyResultDataAccessException.class);

		// Act / Assert
		assertThatCode(() -> repository.findById(id))
			.isInstanceOf(ObjectRetrievalFailureException.class)
			.hasMessageContaining(String.valueOf(id));
	}

	@Test
	void findSpecialtiesByNameIn_existingNames_returnsSpecialtiesList() {
		// Arrange
		Set<String> names = new HashSet<>(Arrays.asList("radiology", "dentistry"));
		Specialty s1 = new Specialty();
		s1.setId(1);
		s1.setName("radiology");
		Specialty s2 = new Specialty();
		s2.setId(2);
		s2.setName("dentistry");
		List<Specialty> expectedList = Arrays.asList(s1, s2);
		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM specialties WHERE specialties.name IN (:names)"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(expectedList);

		// Act
		List<Specialty> actualList = repository.findSpecialtiesByNameIn(names);

		// Assert
		assertThat(actualList).isNotNull();
		assertThat(actualList).hasSize(2);
		assertThat(actualList).extracting("name").containsExactlyInAnyOrder("radiology", "dentistry");
	}

	@Test
	void findSpecialtiesByNameIn_noMatchingNames_returnsEmptyList() {
		// Arrange
		Set<String> names = new HashSet<>(Arrays.asList("unknown1", "unknown2"));
		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM specialties WHERE specialties.name IN (:names)"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(Collections.emptyList());

		// Act
		List<Specialty> actualList = repository.findSpecialtiesByNameIn(names);

		// Assert
		assertThat(actualList).isNotNull();
		assertThat(actualList).isEmpty();
	}

	@Test
	void findAll_returnsAllSpecialties() {
		// Arrange
		Specialty s1 = new Specialty();
		s1.setId(1);
		s1.setName("radiology");
		Specialty s2 = new Specialty();
		s2.setId(2);
		s2.setName("surgery");
		List<Specialty> expectedList = Arrays.asList(s1, s2);
		when(namedParameterJdbcTemplate.query(eq("SELECT id, name FROM specialties"),
				any(Map.class), any(BeanPropertyRowMapper.class))).thenReturn(expectedList);

		// Act
		Collection<Specialty> actualCollection = repository.findAll();

		// Assert
		assertThat(actualCollection).isNotNull();
		assertThat(actualCollection).hasSize(2);
		assertThat(actualCollection).extracting("name").containsExactlyInAnyOrder("radiology", "surgery");
	}

	@Test
	void save_newSpecialty_insertsAndSetsId() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setName("newSpecialty");
		// isNew returns true if id == 0
		specialty.setId(0);
		when(insertSpecialty.executeAndReturnKey(any(BeanPropertySqlParameterSource.class))).thenReturn(10);

		// Act
		repository.save(specialty);

		// Assert
		assertThat(specialty.getId()).isEqualTo(10);
		verify(insertSpecialty, times(1)).executeAndReturnKey(any(BeanPropertySqlParameterSource.class));
		verify(namedParameterJdbcTemplate, times(0)).update(any(String.class), any(BeanPropertySqlParameterSource.class));
	}

	@Test
	void save_existingSpecialty_updatesRecord() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(5);
		specialty.setName("updatedName");

		// Act
		repository.save(specialty);

		// Assert
		verify(namedParameterJdbcTemplate, times(1)).update(eq("UPDATE specialties SET name=:name WHERE id=:id"),
				any(BeanPropertySqlParameterSource.class));
		verify(insertSpecialty, times(0)).executeAndReturnKey(any(BeanPropertySqlParameterSource.class));
	}

	@Test
	void delete_existingSpecialty_deletesFromRelatedTables() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(7);

		// Act
		repository.delete(specialty);

		// Assert
		ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
		verify(namedParameterJdbcTemplate, times(1)).update(eq("DELETE FROM vet_specialties WHERE specialty_id=:id"), captor.capture());
		verify(namedParameterJdbcTemplate, times(1)).update(eq("DELETE FROM specialties WHERE id=:id"), captor.capture());

		List<Map<String, Object>> allParams = captor.getAllValues();
		for (Map<String, Object> params : allParams) {
			assertThat(params).containsEntry("id", 7);
		}
	}

}
