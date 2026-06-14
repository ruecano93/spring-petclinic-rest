package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.Owner;

class JpaOwnerRepositoryImplTest {

	private JpaOwnerRepositoryImpl repository;

	private EntityManager em;

	private Query query;

	private Query countQuery;

	private Pageable pageable;

	@BeforeEach
	void setUp() {
		em = mock(EntityManager.class);
		repository = new JpaOwnerRepositoryImpl();
		repository.em = em;
		query = mock(Query.class);
		countQuery = mock(Query.class);
		pageable = PageRequest.of(0, 10);
	}

	@Test
	void findByLastName_validLastName_returnsOwnersWithPets() {
		// Arrange
		List<Owner> expectedOwners = List.of(new Owner(), new Owner());
		when(em.createQuery("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName"))
			.thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
		when(query.getResultList()).thenReturn(expectedOwners);

		// Act
		var owners = repository.findByLastName("Smith");

		// Assert
		assertThat(owners).isEqualTo(expectedOwners);
		verify(em).createQuery("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName");
		verify(query).setParameter("lastName", "Smith%");
		verify(query).getResultList();
	}

	@Test
	void findByLastName_noOwnersFound_returnsEmptyCollection() {
		// Arrange
		when(em.createQuery("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName"))
			.thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
		when(query.getResultList()).thenReturn(Collections.emptyList());

		// Act
		var owners = repository.findByLastName("Nonexistent");

		// Assert
		assertThat(owners).isEmpty();
		verify(em).createQuery("SELECT DISTINCT owner FROM Owner owner left join fetch owner.pets WHERE owner.lastName LIKE :lastName");
		verify(query).setParameter("lastName", "Nonexistent%");
		verify(query).getResultList();
	}

	@Test
	void findByLastNamePageable_validLastName_returnsPageOfOwners() {
		// Arrange
		List<Owner> expectedOwners = List.of(new Owner(), new Owner());
		when(em.createQuery("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName ORDER BY owner.id")).thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
		when(query.setFirstResult(anyInt())).thenReturn(query);
		when(query.setMaxResults(anyInt())).thenReturn(query);
		when(query.getResultList()).thenReturn(expectedOwners);

		when(em.createQuery("SELECT COUNT(owner) FROM Owner owner WHERE owner.lastName LIKE :lastName")).thenReturn(countQuery);
		when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(2L);

		// Act
		Page<Owner> page = repository.findByLastName("Smith", pageable);

		// Assert
		assertThat(page.getContent()).isEqualTo(expectedOwners);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getPageable()).isEqualTo(pageable);

		verify(em).createQuery("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName ORDER BY owner.id");
		verify(query).setParameter("lastName", "Smith%");
		verify(query).setFirstResult((int) pageable.getOffset());
		verify(query).setMaxResults(pageable.getPageSize());
		verify(query).getResultList();

		verify(em).createQuery("SELECT COUNT(owner) FROM Owner owner WHERE owner.lastName LIKE :lastName");
		verify(countQuery).setParameter("lastName", "Smith%");
		verify(countQuery).getSingleResult();
	}

	@Test
	void findByLastNamePageable_noOwnersFound_returnsEmptyPage() {
		// Arrange
		when(em.createQuery("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName ORDER BY owner.id")).thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
		when(query.setFirstResult(anyInt())).thenReturn(query);
		when(query.setMaxResults(anyInt())).thenReturn(query);
		when(query.getResultList()).thenReturn(Collections.emptyList());

		when(em.createQuery("SELECT COUNT(owner) FROM Owner owner WHERE owner.lastName LIKE :lastName")).thenReturn(countQuery);
		when(countQuery.setParameter(anyString(), any())).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(0L);

		// Act
		Page<Owner> page = repository.findByLastName("Nonexistent", pageable);

		// Assert
		assertThat(page.getContent()).isEmpty();
		assertThat(page.getTotalElements()).isZero();
		assertThat(page.getPageable()).isEqualTo(pageable);

		verify(em).createQuery("SELECT owner FROM Owner owner WHERE owner.lastName LIKE :lastName ORDER BY owner.id");
		verify(query).setParameter("lastName", "Nonexistent%");
		verify(query).setFirstResult((int) pageable.getOffset());
		verify(query).setMaxResults(pageable.getPageSize());
		verify(query).getResultList();

		verify(em).createQuery("SELECT COUNT(owner) FROM Owner owner WHERE owner.lastName LIKE :lastName");
		verify(countQuery).setParameter("lastName", "Nonexistent%");
		verify(countQuery).getSingleResult();
	}

	@Test
	void findById_existingId_returnsOwnerWithPets() {
		// Arrange
		Owner expectedOwner = new Owner();
		when(em.createQuery("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id")).thenReturn(query);
		when(query.setParameter(anyString(), any())).thenReturn(query);
		when(query.getSingleResult()).thenReturn(expectedOwner);

		// Act
		Owner owner = repository.findById(1);

		// Assert
		assertThat(owner).isEqualTo(expectedOwner);
		verify(em).createQuery("SELECT owner FROM Owner owner left join fetch owner.pets WHERE owner.id =:id");
		verify(query).setParameter("id", 1);
		verify(query).getSingleResult();
	}

	@Test
	void save_newOwner_callsPersist() {
		// Arrange
		Owner newOwner = new Owner();
		newOwner.setId(null);

		// Act
		repository.save(newOwner);

		// Assert
		verify(em).persist(newOwner);
		verify(em, times(0)).merge(any());
	}

	@Test
	void save_existingOwner_callsMerge() {
		// Arrange
		Owner existingOwner = new Owner();
		existingOwner.setId(1);

		// Act
		repository.save(existingOwner);

		// Assert
		verify(em).merge(existingOwner);
		verify(em, times(0)).persist(any());
	}

	@Test
	void findAll_returnsAllOwners() {
		// Arrange
		List<Owner> expectedOwners = List.of(new Owner(), new Owner());
		when(em.createQuery("SELECT owner FROM Owner owner")).thenReturn(query);
		when(query.getResultList()).thenReturn(expectedOwners);

		// Act
		var owners = repository.findAll();

		// Assert
		assertThat(owners).isEqualTo(expectedOwners);
		verify(em).createQuery("SELECT owner FROM Owner owner");
		verify(query).getResultList();
	}

	@Test
	void findAllPageable_returnsPageOfOwners() {
		// Arrange
		List<Owner> expectedOwners = List.of(new Owner(), new Owner());
		when(em.createQuery("SELECT owner FROM Owner owner ORDER BY owner.id")).thenReturn(query);
		when(query.setFirstResult(anyInt())).thenReturn(query);
		when(query.setMaxResults(anyInt())).thenReturn(query);
		when(query.getResultList()).thenReturn(expectedOwners);

		when(em.createQuery("SELECT COUNT(owner) FROM Owner owner")).thenReturn(countQuery);
		when(countQuery.getSingleResult()).thenReturn(2L);

		// Act
		Page<Owner> page = repository.findAll(pageable);

		// Assert
		assertThat(page.getContent()).isEqualTo(expectedOwners);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getPageable()).isEqualTo(pageable);

		verify(em).createQuery("SELECT owner FROM Owner owner ORDER BY owner.id");
		verify(query).setFirstResult((int) pageable.getOffset());
		verify(query).setMaxResults(pageable.getPageSize());
		verify(query).getResultList();

		verify(em).createQuery("SELECT COUNT(owner) FROM Owner owner");
		verify(countQuery).getSingleResult();
	}

	@Test
	void delete_existingOwner_callsRemove() {
		// Arrange
		Owner owner = new Owner();
		when(em.contains(owner)).thenReturn(true);

		// Act
		repository.delete(owner);

		// Assert
		verify(em).contains(owner);
		verify(em).remove(owner);
		verify(em, times(0)).merge(any());
	}

	@Test
	void delete_nonManagedOwner_callsMergeAndRemove() {
		// Arrange
		Owner owner = new Owner();
		Owner managedOwner = new Owner();
		when(em.contains(owner)).thenReturn(false);
		when(em.merge(owner)).thenReturn(managedOwner);

		// Act
		repository.delete(owner);

		// Assert
		InOrder inOrder = Mockito.inOrder(em);
		inOrder.verify(em).contains(owner);
		inOrder.verify(em).merge(owner);
		inOrder.verify(em).remove(managedOwner);
	}

}
