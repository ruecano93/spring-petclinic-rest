package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;

class JpaPetRepositoryImplTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @Mock
    private Query countQuery;

    private JpaPetRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        repository = new JpaPetRepositoryImpl();
        repository.em = em;
    }

    @Test
    void findPetTypes_whenCalled_shouldReturnListOfPetTypes() {
        // Arrange
        List<PetType> expectedPetTypes = Arrays.asList(new PetType(), new PetType());
        when(em.createQuery("SELECT ptype FROM PetType ptype ORDER BY ptype.name")).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedPetTypes);

        // Act
        List<PetType> actualPetTypes = repository.findPetTypes();

        // Assert
        assertThat(actualPetTypes).isEqualTo(expectedPetTypes);
        verify(em).createQuery("SELECT ptype FROM PetType ptype ORDER BY ptype.name");
        verify(query).getResultList();
    }

    @Test
    void findById_whenIdExists_shouldReturnPet() {
        // Arrange
        int petId = 1;
        Pet expectedPet = new Pet();
        when(em.find(Pet.class, petId)).thenReturn(expectedPet);

        // Act
        Pet actualPet = repository.findById(petId);

        // Assert
        assertThat(actualPet).isEqualTo(expectedPet);
        verify(em).find(Pet.class, petId);
    }

    @Test
    void findById_whenIdDoesNotExist_shouldReturnNull() {
        // Arrange
        int petId = 999;
        when(em.find(Pet.class, petId)).thenReturn(null);

        // Act
        Pet actualPet = repository.findById(petId);

        // Assert
        assertThat(actualPet).isNull();
        verify(em).find(Pet.class, petId);
    }

    @Test
    void save_whenPetIdIsNull_shouldPersistPet() {
        // Arrange
        Pet pet = new Pet();
        pet.setId(null);

        // Act
        repository.save(pet);

        // Assert
        verify(em).persist(pet);
        verify(em, never()).merge(any());
    }

    @Test
    void save_whenPetIdIsNotNull_shouldMergePet() {
        // Arrange
        Pet pet = new Pet();
        pet.setId(1);

        // Act
        repository.save(pet);

        // Assert
        verify(em).merge(pet);
        verify(em, never()).persist(any());
    }

    @Test
    void findAll_whenCalled_shouldReturnCollectionOfPets() {
        // Arrange
        List<Pet> expectedPets = Arrays.asList(new Pet(), new Pet());
        when(em.createQuery("SELECT pet FROM Pet pet")).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedPets);

        // Act
        Collection<Pet> actualPets = repository.findAll();

        // Assert
        assertThat(actualPets).isEqualTo(expectedPets);
        verify(em).createQuery("SELECT pet FROM Pet pet");
        verify(query).getResultList();
    }

    @Test
    void findAll_withPageable_whenCalled_shouldReturnPageOfPets() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 2);
        List<Pet> expectedPets = Arrays.asList(new Pet(), new Pet());
        long expectedTotal = 5L;

        when(em.createQuery("SELECT pet FROM Pet pet ORDER BY pet.id")).thenReturn(query);
        when(query.setFirstResult(0)).thenReturn(query);
        when(query.setMaxResults(2)).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedPets);

        when(em.createQuery("SELECT COUNT(pet) FROM Pet pet")).thenReturn(countQuery);
        when(countQuery.getSingleResult()).thenReturn(expectedTotal);

        // Act
        Page<Pet> page = repository.findAll(pageable);

        // Assert
        assertThat(page.getContent()).isEqualTo(expectedPets);
        assertThat(page.getPageable()).isEqualTo(pageable);
        assertThat(page.getTotalElements()).isEqualTo(expectedTotal);

        verify(em).createQuery("SELECT pet FROM Pet pet ORDER BY pet.id");
        verify(query).setFirstResult(0);
        verify(query).setMaxResults(2);
        verify(query).getResultList();

        verify(em).createQuery("SELECT COUNT(pet) FROM Pet pet");
        verify(countQuery).getSingleResult();
    }

    @Test
    void delete_whenPetIsManaged_shouldRemovePetAndVisits() {
        // Arrange
        Pet pet = new Pet();
        pet.setId(1);
        String petIdStr = pet.getId().toString();

        Query deleteVisitsQuery = mock(Query.class);
        Query deletePetQuery = mock(Query.class);

        when(em.createQuery("DELETE FROM Visit visit WHERE pet.id=" + petIdStr)).thenReturn(deleteVisitsQuery);
        when(em.createQuery("DELETE FROM Pet pet WHERE id=" + petIdStr)).thenReturn(deletePetQuery);
        when(em.contains(pet)).thenReturn(true);

        // Act
        repository.delete(pet);

        // Assert
        verify(em).createQuery("DELETE FROM Visit visit WHERE pet.id=" + petIdStr);
        verify(deleteVisitsQuery).executeUpdate();

        verify(em).createQuery("DELETE FROM Pet pet WHERE id=" + petIdStr);
        verify(deletePetQuery).executeUpdate();

        verify(em).contains(pet);
        verify(em).remove(pet);
    }

    @Test
    void delete_whenPetIsNotManaged_shouldRemovePetAndVisitsWithoutRemoveCall() {
        // Arrange
        Pet pet = new Pet();
        pet.setId(2);
        String petIdStr = pet.getId().toString();

        Query deleteVisitsQuery = mock(Query.class);
        Query deletePetQuery = mock(Query.class);

        when(em.createQuery("DELETE FROM Visit visit WHERE pet.id=" + petIdStr)).thenReturn(deleteVisitsQuery);
        when(em.createQuery("DELETE FROM Pet pet WHERE id=" + petIdStr)).thenReturn(deletePetQuery);
        when(em.contains(pet)).thenReturn(false);

        // Act
        repository.delete(pet);

        // Assert
        verify(em).createQuery("DELETE FROM Visit visit WHERE pet.id=" + petIdStr);
        verify(deleteVisitsQuery).executeUpdate();

        verify(em).createQuery("DELETE FROM Pet pet WHERE id=" + petIdStr);
        verify(deletePetQuery).executeUpdate();

        verify(em).contains(pet);
        verify(em, never()).remove(any());
    }
}
