package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.OwnerMapper;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.rest.dto.OwnerDto;
import org.springframework.samples.petclinic.rest.dto.OwnerFieldsDto;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.rest.dto.PetFieldsDto;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.rest.dto.VisitFieldsDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.security.test.context.support.WithMockUser;

class OwnerRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    OwnerMapper ownerMapper;

    @Mock
    PetMapper petMapper;

    @Mock
    VisitMapper visitMapper;

    @InjectMocks
    OwnerRestControllerV1 ownerRestControllerV1;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void listOwners_withLastName_returnsOwnerDtoList() {
        // Arrange
        String lastName = "Smith";
        Owner owner = new Owner();
        owner.setId(1);
        Collection<Owner> owners = List.of(owner);
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setId(1);
        when(clinicService.findOwnerByLastName(lastName)).thenReturn(owners);
        when(ownerMapper.toOwnerDtoCollection(owners)).thenReturn(List.of(ownerDto));

        // Act
        ResponseEntity<List<OwnerDto>> response = ownerRestControllerV1.listOwners(lastName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(ownerDto);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void listOwners_withLastName_returnsNotFound_whenNoOwnersFound() {
        // Arrange
        String lastName = "Unknown";
        when(clinicService.findOwnerByLastName(lastName)).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<OwnerDto>> response = ownerRestControllerV1.listOwners(lastName);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void listOwners_withoutLastName_returnsOwnerDtoList() {
        // Arrange
        Collection<Owner> owners = List.of(new Owner());
        OwnerDto ownerDto = new OwnerDto();
        when(clinicService.findAllOwners()).thenReturn(owners);
        when(ownerMapper.toOwnerDtoCollection(owners)).thenReturn(List.of(ownerDto));

        // Act
        ResponseEntity<List<OwnerDto>> response = ownerRestControllerV1.listOwners(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(ownerDto);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void listOwners_withoutLastName_returnsNotFound_whenNoOwnersFound() {
        // Arrange
        when(clinicService.findAllOwners()).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<OwnerDto>> response = ownerRestControllerV1.listOwners(null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void getOwner_existingId_returnsOwnerDto() {
        // Arrange
        Integer ownerId = 1;
        Owner owner = new Owner();
        owner.setId(ownerId);
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setId(ownerId);
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(ownerMapper.toOwnerDto(owner)).thenReturn(ownerDto);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.getOwner(ownerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(ownerDto);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void getOwner_nonExistingId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.getOwner(ownerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void addOwner_validOwnerFields_returnsCreatedOwnerDtoWithLocationHeader() {
        // Arrange
        OwnerFieldsDto ownerFieldsDto = new OwnerFieldsDto();
        Owner owner = new Owner();
        owner.setId(1);
        OwnerDto ownerDto = new OwnerDto();
        ownerDto.setId(1);
        when(ownerMapper.toOwner(ownerFieldsDto)).thenReturn(owner);
        when(ownerMapper.toOwnerDto(owner)).thenReturn(ownerDto);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.addOwner(ownerFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(ownerDto);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/owners/1"));
        verify(clinicService).saveOwner(owner);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void updateOwner_existingId_updatesOwnerAndReturnsNoContent() {
        // Arrange
        Integer ownerId = 1;
        OwnerFieldsDto ownerFieldsDto = new OwnerFieldsDto();
        ownerFieldsDto.setAddress("addr");
        ownerFieldsDto.setCity("city");
        ownerFieldsDto.setFirstName("fn");
        ownerFieldsDto.setLastName("ln");
        ownerFieldsDto.setTelephone("tel");
        Owner currentOwner = new Owner();
        currentOwner.setId(ownerId);
        OwnerDto updatedOwnerDto = new OwnerDto();
        updatedOwnerDto.setId(ownerId);
        when(clinicService.findOwnerById(ownerId)).thenReturn(currentOwner);
        when(ownerMapper.toOwnerDto(currentOwner)).thenReturn(updatedOwnerDto);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.updateOwner(ownerId, ownerFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isEqualTo(updatedOwnerDto);
        assertThat(currentOwner.getAddress()).isEqualTo("addr");
        assertThat(currentOwner.getCity()).isEqualTo("city");
        assertThat(currentOwner.getFirstName()).isEqualTo("fn");
        assertThat(currentOwner.getLastName()).isEqualTo("ln");
        assertThat(currentOwner.getTelephone()).isEqualTo("tel");
        verify(clinicService).saveOwner(currentOwner);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void updateOwner_nonExistingId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        OwnerFieldsDto ownerFieldsDto = new OwnerFieldsDto();
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.updateOwner(ownerId, ownerFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void deleteOwner_existingId_deletesOwnerAndReturnsNoContent() {
        // Arrange
        Integer ownerId = 1;
        Owner owner = new Owner();
        owner.setId(ownerId);
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.deleteOwner(ownerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(clinicService).deleteOwner(owner);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void deleteOwner_nonExistingId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<OwnerDto> response = ownerRestControllerV1.deleteOwner(ownerId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void addPetToOwner_existingOwnerId_returnsCreatedPetDtoWithLocationHeader() {
        // Arrange
        Integer ownerId = 1;
        Owner owner = new Owner();
        owner.setId(ownerId);
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        Pet pet = new Pet();
        pet.setId(2);
        PetDto petDto = new PetDto();
        petDto.setId(2);
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(petMapper.toPet(petFieldsDto)).thenReturn(pet);
        when(petMapper.toPetDto(pet)).thenReturn(petDto);

        // Act
        ResponseEntity<PetDto> response = ownerRestControllerV1.addPetToOwner(ownerId, petFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(petDto);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/pets/2"));
        assertThat(pet.getOwner()).isEqualTo(owner);
        verify(clinicService).savePet(pet);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void addPetToOwner_nonExistingOwnerId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = ownerRestControllerV1.addPetToOwner(ownerId, petFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void updateOwnersPet_existingOwnerAndPetId_updatesPetAndReturnsNoContent() {
        // Arrange
        Integer ownerId = 1;
        Integer petId = 2;
        Owner owner = new Owner();
        owner.setId(ownerId);
        Pet pet = new Pet();
        pet.setId(petId);
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        petFieldsDto.setBirthDate(LocalDate.of(2020, 1, 1));
        petFieldsDto.setName("Fluffy");
        petFieldsDto.setType("dog");
        PetType petType = new PetType();
        petType.setName("dog");
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(clinicService.findPetById(petId)).thenReturn(pet);
        when(petMapper.toPetType("dog")).thenReturn(petType);

        // Act
        ResponseEntity<Void> response = ownerRestControllerV1.updateOwnersPet(ownerId, petId, petFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(pet.getBirthDate()).isEqualTo(LocalDate.of(2020, 1, 1));
        assertThat(pet.getName()).isEqualTo("Fluffy");
        assertThat(pet.getType()).isEqualTo(petType);
        verify(clinicService).savePet(pet);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void updateOwnersPet_nonExistingOwnerId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        Integer petId = 2;
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<Void> response = ownerRestControllerV1.updateOwnersPet(ownerId, petId, petFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void updateOwnersPet_existingOwner_nonExistingPetId_returnsNotFound() {
        // Arrange
        Integer ownerId = 1;
        Integer petId = 99;
        Owner owner = new Owner();
        owner.setId(ownerId);
        PetFieldsDto petFieldsDto = new PetFieldsDto();
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(clinicService.findPetById(petId)).thenReturn(null);

        // Act
        ResponseEntity<Void> response = ownerRestControllerV1.updateOwnersPet(ownerId, petId, petFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void addVisitToOwner_createsVisitAndReturnsCreatedVisitDtoWithLocationHeader() {
        // Arrange
        Integer ownerId = 1;
        Integer petId = 2;
        VisitFieldsDto visitFieldsDto = new VisitFieldsDto();
        Visit visit = new Visit();
        visit.setId(3);
        VisitDto visitDto = new VisitDto();
        visitDto.setId(3);
        when(visitMapper.toVisit(visitFieldsDto)).thenReturn(visit);
        when(visitMapper.toVisitDto(visit)).thenReturn(visitDto);

        // Act
        ResponseEntity<VisitDto> response = ownerRestControllerV1.addVisitToOwner(ownerId, petId, visitFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(visitDto);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/visits/3"));
        assertThat(visit.getPet()).isNotNull();
        assertThat(visit.getPet().getId()).isEqualTo(petId);
        verify(clinicService).saveVisit(visit);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void getOwnersPet_existingOwnerAndPetId_returnsPetDto() {
        // Arrange
        Integer ownerId = 1;
        Integer petId = 2;
        Owner owner = new Owner();
        Pet pet = new Pet();
        pet.setId(petId);
        owner.setId(ownerId);
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(owner.getPet(petId)).thenReturn(pet);
        PetDto petDto = new PetDto();
        petDto.setId(petId);
        when(petMapper.toPetDto(pet)).thenReturn(petDto);

        // Act
        ResponseEntity<PetDto> response = ownerRestControllerV1.getOwnersPet(ownerId, petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(petDto);
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void getOwnersPet_nonExistingOwnerId_returnsNotFound() {
        // Arrange
        Integer ownerId = 99;
        Integer petId = 2;
        when(clinicService.findOwnerById(ownerId)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = ownerRestControllerV1.getOwnersPet(ownerId, petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = "OWNER_ADMIN")
    void getOwnersPet_existingOwner_nonExistingPetId_returnsNotFound() {
        // Arrange
        Integer ownerId = 1;
        Integer petId = 99;
        Owner owner = new Owner();
        owner.setId(ownerId);
        when(clinicService.findOwnerById(ownerId)).thenReturn(owner);
        when(owner.getPet(petId)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = ownerRestControllerV1.getOwnersPet(ownerId, petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
