package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.samples.petclinic.model.Specialty;

class JpaSpecialtyRepositoryImplTest {

	private JpaSpecialtyRepositoryImpl repository;

	private EntityManager entityManager;

	@BeforeEach
	void setUp() {
		entityManager = mock(EntityManager.class);
		repository = new JpaSpecialtyRepositoryImpl();
		// inject mock EntityManager
		org.springframework.test.util.ReflectionTestUtils.setField(repository, "em", entityManager);
	}

	@Test
	void findById_existingId_returnsSpecialty() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(1);
		when(entityManager.find(Specialty.class, 1)).thenReturn(specialty);

		// Act
		Specialty result = repository.findById(1);

		// Assert
		assertThat(result).isSameAs(specialty);
		verify(entityManager).find(Specialty.class, 1);
	}

	@Test
	void findById_nonExistingId_returnsNull() {
		// Arrange
		when(entityManager.find(Specialty.class, 999)).thenReturn(null);

		// Act
		Specialty result = repository.findById(999);

		// Assert
		assertThat(result).isNull();
		verify(entityManager).find(Specialty.class, 999);
	}

	@Test
	void findSpecialtiesByNameIn_validNames_returnsListOfSpecialties() {
		// Arrange
		Set<String> names = new HashSet<>(Arrays.asList("name1", "name2"));
		TypedQuery<Specialty> query = mock(TypedQuery.class);
		List<Specialty> specialties = Arrays.asList(new Specialty(), new Specialty());
		when(entityManager.createQuery("SELECT s FROM Specialty s WHERE s.name IN :names", Specialty.class)).thenReturn(query);
		when(query.setParameter("names", names)).thenReturn(query);
		when(query.getResultList()).thenReturn(specialties);

		// Act
		List<Specialty> result = repository.findSpecialtiesByNameIn(names);

		// Assert
		assertThat(result).isSameAs(specialties);
		verify(entityManager).createQuery("SELECT s FROM Specialty s WHERE s.name IN :names", Specialty.class);
		verify(query).setParameter("names", names);
		verify(query).getResultList();
	}

	@Test
	void findSpecialtiesByNameIn_emptySet_returnsEmptyList() {
		// Arrange
		Set<String> names = Collections.emptySet();
		TypedQuery<Specialty> query = mock(TypedQuery.class);
		List<Specialty> specialties = Collections.emptyList();
		when(entityManager.createQuery("SELECT s FROM Specialty s WHERE s.name IN :names", Specialty.class)).thenReturn(query);
		when(query.setParameter("names", names)).thenReturn(query);
		when(query.getResultList()).thenReturn(specialties);

		// Act
		List<Specialty> result = repository.findSpecialtiesByNameIn(names);

		// Assert
		assertThat(result).isEmpty();
		verify(entityManager).createQuery("SELECT s FROM Specialty s WHERE s.name IN :names", Specialty.class);
		verify(query).setParameter("names", names);
		verify(query).getResultList();
	}

	@Test
	void findAll_returnsAllSpecialties() {
		// Arrange
		Query query = mock(Query.class);
		List<Specialty> specialties = Arrays.asList(new Specialty(), new Specialty());
		when(entityManager.createQuery("SELECT s FROM Specialty s")).thenReturn(query);
		when(query.getResultList()).thenReturn(specialties);

		// Act
		@SuppressWarnings("unchecked")
		List<Specialty> result = (List<Specialty>) repository.findAll();

		// Assert
		assertThat(result).isSameAs(specialties);
		verify(entityManager).createQuery("SELECT s FROM Specialty s");
		verify(query).getResultList();
	}

	@Test
	void save_newSpecialty_callsPersist() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(null);

		// Act
		repository.save(specialty);

		// Assert
		verify(entityManager).persist(specialty);
		verify(entityManager, never()).merge(any());
	}

	@Test
	void save_existingSpecialty_callsMerge() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(10);

		// Act
		repository.save(specialty);

		// Assert
		verify(entityManager).merge(specialty);
		verify(entityManager, never()).persist(any());
	}

	@Test
	void delete_existingSpecialty_removesAndDeletesFromJoinAndEntity() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(5);
		when(entityManager.contains(specialty)).thenReturn(true);
		Query nativeQuery = mock(Query.class);
		Query deleteQuery = mock(Query.class);
		when(entityManager.createNativeQuery("DELETE FROM vet_specialties WHERE specialty_id=5")).thenReturn(nativeQuery);
		when(entityManager.createQuery("DELETE FROM Specialty specialty WHERE id=5")).thenReturn(deleteQuery);
		when(nativeQuery.executeUpdate()).thenReturn(1);
		when(deleteQuery.executeUpdate()).thenReturn(1);
		doNothing().when(entityManager).remove(specialty);

		// Act
		repository.delete(specialty);

		// Assert
		InOrder inOrder = Mockito.inOrder(entityManager, nativeQuery, deleteQuery);
		inOrder.verify(entityManager).contains(specialty);
		inOrder.verify(entityManager).remove(specialty);
		inOrder.verify(entityManager).createNativeQuery("DELETE FROM vet_specialties WHERE specialty_id=5");
		inOrder.verify(nativeQuery).executeUpdate();
		inOrder.verify(entityManager).createQuery("DELETE FROM Specialty specialty WHERE id=5");
		inOrder.verify(deleteQuery).executeUpdate();
	}

	@Test
	void delete_nonContainedSpecialty_mergesThenRemovesAndDeletes() {
		// Arrange
		Specialty specialty = new Specialty();
		specialty.setId(7);
		Specialty mergedSpecialty = new Specialty();
		mergedSpecialty.setId(7);
		when(entityManager.contains(specialty)).thenReturn(false);
		when(entityManager.merge(specialty)).thenReturn(mergedSpecialty);
		Query nativeQuery = mock(Query.class);
		Query deleteQuery = mock(Query.class);
		when(entityManager.createNativeQuery("DELETE FROM vet_specialties WHERE specialty_id=7")).thenReturn(nativeQuery);
		when(entityManager.createQuery("DELETE FROM Specialty specialty WHERE id=7")).thenReturn(deleteQuery);
		when(nativeQuery.executeUpdate()).thenReturn(1);
		when(deleteQuery.executeUpdate()).thenReturn(1);
		doNothing().when(entityManager).remove(mergedSpecialty);

		// Act
		repository.delete(specialty);

		// Assert
		InOrder inOrder = Mockito.inOrder(entityManager, nativeQuery, deleteQuery);
		inOrder.verify(entityManager).contains(specialty);
		inOrder.verify(entityManager).merge(specialty);
		inOrder.verify(entityManager).remove(mergedSpecialty);
		inOrder.verify(entityManager).createNativeQuery("DELETE FROM vet_specialties WHERE specialty_id=7");
		inOrder.verify(nativeQuery).executeUpdate();
		inOrder.verify(entityManager).createQuery("DELETE FROM Specialty specialty WHERE id=7");
		inOrder.verify(deleteQuery).executeUpdate();
	}

}
