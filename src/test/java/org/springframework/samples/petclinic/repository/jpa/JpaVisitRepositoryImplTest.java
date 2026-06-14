package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.ArrayList;
import java.util.Collection;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.samples.petclinic.model.Visit;

class JpaVisitRepositoryImplTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private JpaVisitRepositoryImpl repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void save_newVisit_callsPersist() {
        // Arrange
        Visit visit = new Visit();
        visit.setId(null);

        // Act
        repository.save(visit);

        // Assert
        verify(em).persist(visit);
    }

    @Test
    void save_existingVisit_callsMerge() {
        // Arrange
        Visit visit = new Visit();
        visit.setId(1);

        // Act
        repository.save(visit);

        // Assert
        verify(em).merge(visit);
    }

    @Test
    void findByPetId_existingPetId_returnsVisitList() {
        // Arrange
        Integer petId = 1;
        List<Visit> expectedVisits = new ArrayList<>();
        expectedVisits.add(new Visit());
        when(em.createQuery("SELECT v FROM Visit v where v.pet.id= :id")).thenReturn(query);
        when(query.setParameter("id", petId)).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedVisits);

        // Act
        List<Visit> actualVisits = repository.findByPetId(petId);

        // Assert
        assertThat(actualVisits).isEqualTo(expectedVisits);
    }

    @Test
    void findByPetId_nonExistingPetId_returnsEmptyList() {
        // Arrange
        Integer petId = 999;
        List<Visit> emptyList = new ArrayList<>();
        when(em.createQuery("SELECT v FROM Visit v where v.pet.id= :id")).thenReturn(query);
        when(query.setParameter("id", petId)).thenReturn(query);
        when(query.getResultList()).thenReturn(emptyList);

        // Act
        List<Visit> actualVisits = repository.findByPetId(petId);

        // Assert
        assertThat(actualVisits).isEmpty();
    }

    @Test
    void findById_existingId_returnsVisit() {
        // Arrange
        int id = 1;
        Visit expectedVisit = new Visit();
        when(em.find(Visit.class, id)).thenReturn(expectedVisit);

        // Act
        Visit actualVisit = repository.findById(id);

        // Assert
        assertThat(actualVisit).isEqualTo(expectedVisit);
    }

    @Test
    void findById_nonExistingId_returnsNull() {
        // Arrange
        int id = 999;
        when(em.find(Visit.class, id)).thenReturn(null);

        // Act
        Visit actualVisit = repository.findById(id);

        // Assert
        assertThat(actualVisit).isNull();
    }

    @Test
    void findAll_returnsListOfVisits() {
        // Arrange
        List<Visit> expectedVisits = new ArrayList<>();
        expectedVisits.add(new Visit());
        when(em.createQuery("SELECT v FROM Visit v")).thenReturn(query);
        when(query.getResultList()).thenReturn(expectedVisits);

        // Act
        Collection<Visit> actualVisits = repository.findAll();

        // Assert
        assertThat(actualVisits).isEqualTo(expectedVisits);
    }

    @Test
    void delete_visitManaged_callsRemoveDirectly() {
        // Arrange
        Visit visit = new Visit();
        when(em.contains(visit)).thenReturn(true);

        // Act
        repository.delete(visit);

        // Assert
        verify(em).remove(visit);
    }

    @Test
    void delete_visitNotManaged_callsMergeThenRemove() {
        // Arrange
        Visit visit = new Visit();
        Visit managedVisit = new Visit();
        when(em.contains(visit)).thenReturn(false);
        when(em.merge(visit)).thenReturn(managedVisit);

        // Act
        repository.delete(visit);

        // Assert
        verify(em).merge(visit);
        verify(em).remove(managedVisit);
    }

}
