package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;

class JpaPetTypeRepositoryImplTest {

	@Mock
	private EntityManager em;

	private JpaPetTypeRepositoryImpl repository;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		repository = new JpaPetTypeRepositoryImpl();
		repository.em = em;
	}

	@Test
	void findById_existingId_returnsPetType() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(1);
		when(em.find(PetType.class, 1)).thenReturn(petType);

		// Act
		PetType result = repository.findById(1);

		// Assert
		assertThat(result).isSameAs(petType);
		verify(em).find(PetType.class, 1);
	}

	@Test
	void findById_nonExistingId_returnsNull() {
		// Arrange
		when(em.find(PetType.class, 99)).thenReturn(null);

		// Act
		PetType result = repository.findById(99);

		// Assert
		assertThat(result).isNull();
		verify(em).find(PetType.class, 99);
	}

	@Test
	void findByName_existingName_returnsPetType() {
		// Arrange
		String name = "dog";
		PetType petType = new PetType();
		petType.setName(name);
		TypedQuery<PetType> query = mock(TypedQuery.class);
		when(em.createQuery("SELECT p FROM PetType p WHERE p.name = :name", PetType.class)).thenReturn(query);
		when(query.setParameter("name", name)).thenReturn(query);
		when(query.getSingleResult()).thenReturn(petType);

		// Act
		PetType result = repository.findByName(name);

		// Assert
		assertThat(result).isSameAs(petType);
		verify(em).createQuery("SELECT p FROM PetType p WHERE p.name = :name", PetType.class);
		verify(query).setParameter("name", name);
		verify(query).getSingleResult();
	}

	@Test
	void findByName_nonExistingName_throwsNoResultException() {
		// Arrange
		String name = "nonexistent";
		TypedQuery<PetType> query = mock(TypedQuery.class);
		when(em.createQuery("SELECT p FROM PetType p WHERE p.name = :name", PetType.class)).thenReturn(query);
		when(query.setParameter("name", name)).thenReturn(query);
		when(query.getSingleResult()).thenThrow(new NoResultException());

		// Act / Assert
		assertThatCode(() -> repository.findByName(name)).isInstanceOf(NoResultException.class);
		verify(em).createQuery("SELECT p FROM PetType p WHERE p.name = :name", PetType.class);
		verify(query).setParameter("name", name);
		verify(query).getSingleResult();
	}

	@Test
	void findAll_returnsListOfPetTypes() {
		// Arrange
		List<PetType> petTypes = new ArrayList<>();
		petTypes.add(new PetType());
		TypedQuery<PetType> query = mock(TypedQuery.class);
		when(em.createQuery("SELECT ptype FROM PetType ptype")).thenReturn(query);
		when(query.getResultList()).thenReturn(petTypes);

		// Act
		Collection<PetType> result = repository.findAll();

		// Assert
		assertThat(result).isSameAs(petTypes);
		verify(em).createQuery("SELECT ptype FROM PetType ptype");
		verify(query).getResultList();
	}

	@Test
	void save_newPetType_callsPersist() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(null);

		// Act
		repository.save(petType);

		// Assert
		verify(em).persist(petType);
		verify(em, never()).merge(any());
	}

	@Test
	void save_existingPetType_callsMerge() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(1);

		// Act
		repository.save(petType);

		// Assert
		verify(em).merge(petType);
		verify(em, never()).persist(any());
	}

	@Test
	void delete_existingPetType_removesPetTypeAndRelatedEntities() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(1);
		when(em.contains(petType)).thenReturn(true);

		Pet pet1 = new Pet();
		pet1.setId(10);
		Visit visit1 = new Visit();
		visit1.setId(100);
		Visit visit2 = new Visit();
		visit2.setId(101);
		List<Visit> visits = new ArrayList<>();
		visits.add(visit1);
		visits.add(visit2);
		pet1.setVisits(visits);

		List<Pet> pets = new ArrayList<>();
		pets.add(pet1);

		TypedQuery<Pet> petQuery = mock(TypedQuery.class);
		when(em.createQuery("SELECT pet FROM Pet pet WHERE type.id=" + petType.getId())).thenReturn(petQuery);
		when(petQuery.getResultList()).thenReturn(pets);

		TypedQuery<?> deleteVisitQuery1 = mock(TypedQuery.class);
		TypedQuery<?> deleteVisitQuery2 = mock(TypedQuery.class);
		TypedQuery<?> deletePetQuery = mock(TypedQuery.class);
		TypedQuery<?> deletePetTypeQuery = mock(TypedQuery.class);

		when(em.createQuery("DELETE FROM Visit visit WHERE id=" + visit1.getId())).thenReturn(deleteVisitQuery1);
		when(em.createQuery("DELETE FROM Visit visit WHERE id=" + visit2.getId())).thenReturn(deleteVisitQuery2);
		when(em.createQuery("DELETE FROM Pet pet WHERE id=" + pet1.getId())).thenReturn(deletePetQuery);
		when(em.createQuery("DELETE FROM PetType pettype WHERE id=" + petType.getId())).thenReturn(deletePetTypeQuery);

		// Act
		repository.delete(petType);

		// Assert
		verify(em).remove(petType);
		verify(deleteVisitQuery1).executeUpdate();
		verify(deleteVisitQuery2).executeUpdate();
		verify(deletePetQuery).executeUpdate();
		verify(deletePetTypeQuery).executeUpdate();
	}

	@Test
	void delete_petTypeNotContained_mergesAndRemovesPetTypeAndRelatedEntities() {
		// Arrange
		PetType petType = new PetType();
		petType.setId(2);
		when(em.contains(petType)).thenReturn(false);

		PetType mergedPetType = new PetType();
		mergedPetType.setId(2);
		when(em.merge(petType)).thenReturn(mergedPetType);

		Pet pet1 = new Pet();
		pet1.setId(20);
		Visit visit1 = new Visit();
		visit1.setId(200);
		List<Visit> visits = new ArrayList<>();
		visits.add(visit1);
		pet1.setVisits(visits);

		List<Pet> pets = new ArrayList<>();
		pets.add(pet1);

		TypedQuery<Pet> petQuery = mock(TypedQuery.class);
		when(em.createQuery("SELECT pet FROM Pet pet WHERE type.id=" + petType.getId())).thenReturn(petQuery);
		when(petQuery.getResultList()).thenReturn(pets);

		TypedQuery<?> deleteVisitQuery = mock(TypedQuery.class);
		TypedQuery<?> deletePetQuery = mock(TypedQuery.class);
		TypedQuery<?> deletePetTypeQuery = mock(TypedQuery.class);

		when(em.createQuery("DELETE FROM Visit visit WHERE id=" + visit1.getId())).thenReturn(deleteVisitQuery);
		when(em.createQuery("DELETE FROM Pet pet WHERE id=" + pet1.getId())).thenReturn(deletePetQuery);
		when(em.createQuery("DELETE FROM PetType pettype WHERE id=" + petType.getId())).thenReturn(deletePetTypeQuery);

		// Act
		repository.delete(petType);

		// Assert
		verify(em).merge(petType);
		verify(em).remove(mergedPetType);
		verify(deleteVisitQuery).executeUpdate();
		verify(deletePetQuery).executeUpdate();
		verify(deletePetTypeQuery).executeUpdate();
	}

}
