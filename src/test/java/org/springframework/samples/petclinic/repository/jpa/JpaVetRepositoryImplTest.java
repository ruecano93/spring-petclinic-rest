package org.springframework.samples.petclinic.repository.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.samples.petclinic.model.Vet;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JpaVetRepositoryImplTest {

    @Mock
    private EntityManager em;

    @Mock
    private Query query;

    @InjectMocks
    private JpaVetRepositoryImpl repository;

    @Captor
    private ArgumentCaptor<Vet> vetCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findById_existingId_returnsVet() {
        // Arrange
        int id = 1;
        Vet vet = new Vet();
        vet.setId(id);
        when(em.find(Vet.class, id)).thenReturn(vet);

        // Act
        Vet found = repository.findById(id);

        // Assert
        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(id);
        verify(em).find(Vet.class, id);
    }

    @Test
    void findById_nonExistingId_returnsNull() {
        // Arrange
        int id = 999;
        when(em.find(Vet.class, id)).thenReturn(null);

        // Act
        Vet found = repository.findById(id);

        // Assert
        assertThat(found).isNull();
        verify(em).find(Vet.class, id);
    }

    @Test
    void findAll_returnsListOfVets() {
        // Arrange
        Vet vet1 = new Vet();
        vet1.setId(1);
        Vet vet2 = new Vet();
        vet2.setId(2);
        List<Vet> vets = List.of(vet1, vet2);
        when(em.createQuery("SELECT vet FROM Vet vet")).thenReturn(query);
        when(query.getResultList()).thenReturn(vets);

        // Act
        var result = repository.findAll();

        // Assert
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2).containsExactlyElementsOf(vets);
        verify(em).createQuery("SELECT vet FROM Vet vet");
        verify(query).getResultList();
    }

    @Test
    void save_newVet_callsPersist() {
        // Arrange
        Vet newVet = new Vet();
        newVet.setId(null);

        // Act
        repository.save(newVet);

        // Assert
        verify(em).persist(newVet);
        verify(em, never()).merge(any());
    }

    @Test
    void save_existingVet_callsMerge() {
        // Arrange
        Vet existingVet = new Vet();
        existingVet.setId(1);

        // Act
        repository.save(existingVet);

        // Assert
        verify(em).merge(existingVet);
        verify(em, never()).persist(any());
    }

    @Test
    void delete_vetManaged_callsRemoveDirectly() {
        // Arrange
        Vet vet = new Vet();
        vet.setId(1);
        when(em.contains(vet)).thenReturn(true);

        // Act
        repository.delete(vet);

        // Assert
        verify(em).contains(vet);
        verify(em).remove(vet);
        verify(em, never()).merge(any());
    }

    @Test
    void delete_vetNotManaged_callsMergeThenRemove() {
        // Arrange
        Vet vet = new Vet();
        vet.setId(1);
        Vet mergedVet = new Vet();
        mergedVet.setId(1);
        when(em.contains(vet)).thenReturn(false);
        when(em.merge(vet)).thenReturn(mergedVet);

        // Act
        repository.delete(vet);

        // Assert
        verify(em).contains(vet);
        verify(em).merge(vet);
        verify(em).remove(mergedVet);
    }
}
