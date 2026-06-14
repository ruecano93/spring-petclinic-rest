package org.springframework.samples.petclinic.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.orm.ObjectRetrievalFailureException;
import org.springframework.samples.petclinic.model.*;
import org.springframework.samples.petclinic.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ClinicServiceImplTest {

    @Mock
    PetRepository petRepository;

    @Mock
    VetRepository vetRepository;

    @Mock
    OwnerRepository ownerRepository;

    @Mock
    VisitRepository visitRepository;

    @Mock
    SpecialtyRepository specialtyRepository;

    @Mock
    PetTypeRepository petTypeRepository;

    @InjectMocks
    ClinicServiceImpl clinicService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void findAllPets_whenCalled_shouldReturnAllPets() {
        List<Pet> pets = List.of(new Pet(), new Pet());
        when(petRepository.findAll()).thenReturn(pets);

        Collection<Pet> result = clinicService.findAllPets();

        assertThat(result).isEqualTo(pets);
        verify(petRepository).findAll();
    }

    @Test
    void findPets_whenPageableProvided_shouldReturnPagedPets() {
        Pageable pageable = mock(Pageable.class);
        Page<Pet> page = new PageImpl<>(List.of(new Pet(), new Pet()));
        when(petRepository.findAll(pageable)).thenReturn(page);

        Page<Pet> result = clinicService.findPets(pageable);

        assertThat(result).isEqualTo(page);
        verify(petRepository).findAll(pageable);
    }

    @Test
    void deletePet_whenPetProvided_shouldCallRepositoryDelete() {
        Pet pet = new Pet();

        clinicService.deletePet(pet);

        verify(petRepository).delete(pet);
    }

    @Test
    void findVisitById_whenVisitExists_shouldReturnVisit() {
        int id = 1;
        Visit visit = new Visit();
        when(visitRepository.findById(id)).thenReturn(visit);

        Visit result = clinicService.findVisitById(id);

        assertThat(result).isEqualTo(visit);
        verify(visitRepository).findById(id);
    }

    @Test
    void findVisitById_whenVisitNotFound_shouldReturnNull() {
        int id = 1;
        when(visitRepository.findById(id)).thenThrow(new ObjectRetrievalFailureException(Visit.class, id));

        Visit result = clinicService.findVisitById(id);

        assertThat(result).isNull();
        verify(visitRepository).findById(id);
    }

    @Test
    void findAllVisits_whenCalled_shouldReturnAllVisits() {
        List<Visit> visits = List.of(new Visit(), new Visit());
        when(visitRepository.findAll()).thenReturn(visits);

        Collection<Visit> result = clinicService.findAllVisits();

        assertThat(result).isEqualTo(visits);
        verify(visitRepository).findAll();
    }

    @Test
    void deleteVisit_whenVisitProvided_shouldCallRepositoryDelete() {
        Visit visit = new Visit();

        clinicService.deleteVisit(visit);

        verify(visitRepository).delete(visit);
    }

    @Test
    void findVetById_whenVetExists_shouldReturnVet() {
        int id = 1;
        Vet vet = new Vet();
        when(vetRepository.findById(id)).thenReturn(vet);

        Vet result = clinicService.findVetById(id);

        assertThat(result).isEqualTo(vet);
        verify(vetRepository).findById(id);
    }

    @Test
    void findVetById_whenVetNotFound_shouldReturnNull() {
        int id = 1;
        when(vetRepository.findById(id)).thenThrow(new EmptyResultDataAccessException(1));

        Vet result = clinicService.findVetById(id);

        assertThat(result).isNull();
        verify(vetRepository).findById(id);
    }

    @Test
    void findAllVets_whenCalled_shouldReturnAllVets() {
        List<Vet> vets = List.of(new Vet(), new Vet());
        when(vetRepository.findAll()).thenReturn(vets);

        Collection<Vet> result = clinicService.findAllVets();

        assertThat(result).isEqualTo(vets);
        verify(vetRepository).findAll();
    }

    @Test
    void saveVet_whenVetProvided_shouldCallRepositorySave() {
        Vet vet = new Vet();

        clinicService.saveVet(vet);

        verify(vetRepository).save(vet);
    }

    @Test
    void deleteVet_whenVetProvided_shouldCallRepositoryDelete() {
        Vet vet = new Vet();

        clinicService.deleteVet(vet);

        verify(vetRepository).delete(vet);
    }

    @Test
    void findAllOwners_whenCalled_shouldReturnAllOwners() {
        List<Owner> owners = List.of(new Owner(), new Owner());
        when(ownerRepository.findAll()).thenReturn(owners);

        Collection<Owner> result = clinicService.findAllOwners();

        assertThat(result).isEqualTo(owners);
        verify(ownerRepository).findAll();
    }

    @Test
    void findOwners_whenLastNameProvided_shouldReturnOwnersByLastName() {
        String lastName = "Smith";
        Pageable pageable = mock(Pageable.class);
        Page<Owner> page = new PageImpl<>(List.of(new Owner(), new Owner()));
        when(ownerRepository.findByLastName(lastName, pageable)).thenReturn(page);

        Page<Owner> result = clinicService.findOwners(lastName, pageable);

        assertThat(result).isEqualTo(page);
        verify(ownerRepository).findByLastName(lastName, pageable);
    }

    @Test
    void findOwners_whenLastNameNull_shouldReturnAllOwnersPaged() {
        Pageable pageable = mock(Pageable.class);
        Page<Owner> page = new PageImpl<>(List.of(new Owner(), new Owner()));
        when(ownerRepository.findAll(pageable)).thenReturn(page);

        Page<Owner> result = clinicService.findOwners(null, pageable);

        assertThat(result).isEqualTo(page);
        verify(ownerRepository).findAll(pageable);
    }

    @Test
    void deleteOwner_whenOwnerProvided_shouldCallRepositoryDelete() {
        Owner owner = new Owner();

        clinicService.deleteOwner(owner);

        verify(ownerRepository).delete(owner);
    }

    @Test
    void findPetTypeById_whenPetTypeExists_shouldReturnPetType() {
        int id = 1;
        PetType petType = new PetType();
        when(petTypeRepository.findById(id)).thenReturn(petType);

        PetType result = clinicService.findPetTypeById(id);

        assertThat(result).isEqualTo(petType);
        verify(petTypeRepository).findById(id);
    }

    @Test
    void findPetTypeById_whenPetTypeNotFound_shouldReturnNull() {
        int id = 1;
        when(petTypeRepository.findById(id)).thenThrow(new ObjectRetrievalFailureException(PetType.class, id));

        PetType result = clinicService.findPetTypeById(id);

        assertThat(result).isNull();
        verify(petTypeRepository).findById(id);
    }

    @Test
    void findAllPetTypes_whenCalled_shouldReturnAllPetTypes() {
        List<PetType> petTypes = List.of(new PetType(), new PetType());
        when(petTypeRepository.findAll()).thenReturn(petTypes);

        Collection<PetType> result = clinicService.findAllPetTypes();

        assertThat(result).isEqualTo(petTypes);
        verify(petTypeRepository).findAll();
    }

    @Test
    void savePetType_whenPetTypeProvided_shouldCallRepositorySave() {
        PetType petType = new PetType();

        clinicService.savePetType(petType);

        verify(petTypeRepository).save(petType);
    }

    @Test
    void deletePetType_whenPetTypeProvided_shouldCallRepositoryDelete() {
        PetType petType = new PetType();

        clinicService.deletePetType(petType);

        verify(petTypeRepository).delete(petType);
    }

    @Test
    void findSpecialtyById_whenSpecialtyExists_shouldReturnSpecialty() {
        int id = 1;
        Specialty specialty = new Specialty();
        when(specialtyRepository.findById(id)).thenReturn(specialty);

        Specialty result = clinicService.findSpecialtyById(id);

        assertThat(result).isEqualTo(specialty);
        verify(specialtyRepository).findById(id);
    }

    @Test
    void findSpecialtyById_whenSpecialtyNotFound_shouldReturnNull() {
        int id = 1;
        when(specialtyRepository.findById(id)).thenThrow(new EmptyResultDataAccessException(1));

        Specialty result = clinicService.findSpecialtyById(id);

        assertThat(result).isNull();
        verify(specialtyRepository).findById(id);
    }

    @Test
    void findAllSpecialties_whenCalled_shouldReturnAllSpecialties() {
        List<Specialty> specialties = List.of(new Specialty(), new Specialty());
        when(specialtyRepository.findAll()).thenReturn(specialties);

        Collection<Specialty> result = clinicService.findAllSpecialties();

        assertThat(result).isEqualTo(specialties);
        verify(specialtyRepository).findAll();
    }

    @Test
    void saveSpecialty_whenSpecialtyProvided_shouldCallRepositorySave() {
        Specialty specialty = new Specialty();

        clinicService.saveSpecialty(specialty);

        verify(specialtyRepository).save(specialty);
    }

    @Test
    void deleteSpecialty_whenSpecialtyProvided_shouldCallRepositoryDelete() {
        Specialty specialty = new Specialty();

        clinicService.deleteSpecialty(specialty);

        verify(specialtyRepository).delete(specialty);
    }

    @Test
    void findPetTypes_whenCalled_shouldReturnPetTypesFromPetRepository() {
        List<PetType> petTypes = List.of(new PetType(), new PetType());
        when(petRepository.findPetTypes()).thenReturn(petTypes);

        Collection<PetType> result = clinicService.findPetTypes();

        assertThat(result).isEqualTo(petTypes);
        verify(petRepository).findPetTypes();
    }

    @Test
    void findOwnerById_whenOwnerExists_shouldReturnOwner() {
        int id = 1;
        Owner owner = new Owner();
        when(ownerRepository.findById(id)).thenReturn(owner);

        Owner result = clinicService.findOwnerById(id);

        assertThat(result).isEqualTo(owner);
        verify(ownerRepository).findById(id);
    }

    @Test
    void findOwnerById_whenOwnerNotFound_shouldReturnNull() {
        int id = 1;
        when(ownerRepository.findById(id)).thenThrow(new ObjectRetrievalFailureException(Owner.class, id));

        Owner result = clinicService.findOwnerById(id);

        assertThat(result).isNull();
        verify(ownerRepository).findById(id);
    }

    @Test
    void findPetById_whenPetExists_shouldReturnPet() {
        int id = 1;
        Pet pet = new Pet();
        when(petRepository.findById(id)).thenReturn(pet);

        Pet result = clinicService.findPetById(id);

        assertThat(result).isEqualTo(pet);
        verify(petRepository).findById(id);
    }

    @Test
    void findPetById_whenPetNotFound_shouldReturnNull() {
        int id = 1;
        when(petRepository.findById(id)).thenThrow(new EmptyResultDataAccessException(1));

        Pet result = clinicService.findPetById(id);

        assertThat(result).isNull();
        verify(petRepository).findById(id);
    }

    @Test
    void savePet_whenPetProvided_shouldSetTypeAndCallRepositorySave() {
        Pet pet = mock(Pet.class);
        PetType petType = new PetType();
        int petTypeId = 42;
        when(pet.getType()).thenReturn(petType);
        when(petType.getId()).thenReturn(petTypeId);
        when(petTypeRepository.findById(petTypeId)).thenReturn(petType);

        clinicService.savePet(pet);

        verify(petTypeRepository).findById(petTypeId);
        verify(pet).setType(petType);
        verify(petRepository).save(pet);
    }

    @Test
    void saveVisit_whenVisitProvided_shouldCallRepositorySave() {
        Visit visit = new Visit();

        clinicService.saveVisit(visit);

        verify(visitRepository).save(visit);
    }

    @Test
    void findVets_whenCalled_shouldReturnAllVets() {
        List<Vet> vets = List.of(new Vet(), new Vet());
        when(vetRepository.findAll()).thenReturn(vets);

        Collection<Vet> result = clinicService.findVets();

        assertThat(result).isEqualTo(vets);
        verify(vetRepository).findAll();
    }

    @Test
    void saveOwner_whenOwnerProvided_shouldCallRepositorySave() {
        Owner owner = new Owner();

        clinicService.saveOwner(owner);

        verify(ownerRepository).save(owner);
    }

    @Test
    void findOwnerByLastName_whenLastNameProvided_shouldReturnOwners() {
        String lastName = "Smith";
        List<Owner> owners = List.of(new Owner(), new Owner());
        when(ownerRepository.findByLastName(lastName)).thenReturn(owners);

        Collection<Owner> result = clinicService.findOwnerByLastName(lastName);

        assertThat(result).isEqualTo(owners);
        verify(ownerRepository).findByLastName(lastName);
    }

    @Test
    void findVisitsByPetId_whenPetIdProvided_shouldReturnVisits() {
        int petId = 1;
        List<Visit> visits = List.of(new Visit(), new Visit());
        when(visitRepository.findByPetId(petId)).thenReturn(visits);

        Collection<Visit> result = clinicService.findVisitsByPetId(petId);

        assertThat(result).isEqualTo(visits);
        verify(visitRepository).findByPetId(petId);
    }

    @Test
    void findSpecialtiesByNameIn_whenNamesProvided_shouldReturnSpecialties() {
        Set<String> names = Set.of("s1", "s2");
        List<Specialty> specialties = List.of(new Specialty(), new Specialty());
        when(specialtyRepository.findSpecialtiesByNameIn(names)).thenReturn(specialties);

        List<Specialty> result = clinicService.findSpecialtiesByNameIn(names);

        assertThat(result).isEqualTo(specialties);
        verify(specialtyRepository).findSpecialtiesByNameIn(names);
    }

    @Test
    void findSpecialtiesByNameIn_whenRepositoryThrowsNotFound_shouldReturnNull() {
        Set<String> names = Set.of("s1", "s2");
        when(specialtyRepository.findSpecialtiesByNameIn(names)).thenThrow(new ObjectRetrievalFailureException(Specialty.class, names));

        List<Specialty> result = clinicService.findSpecialtiesByNameIn(names);

        assertThat(result).isNull();
        verify(specialtyRepository).findSpecialtiesByNameIn(names);
    }

}
