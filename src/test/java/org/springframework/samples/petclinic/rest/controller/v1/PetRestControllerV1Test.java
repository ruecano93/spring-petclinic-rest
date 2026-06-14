package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetDto;
import org.springframework.samples.petclinic.service.ClinicService;

class PetRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    PetMapper petMapper;

    @InjectMocks
    PetRestControllerV1 petRestControllerV1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getPet_petExists_returnsPetDtoWithStatusOk() {
        // Arrange
        Integer petId = 1;
        Pet pet = new Pet();
        PetDto petDto = new PetDto();
        when(clinicService.findPetById(petId)).thenReturn(pet);
        when(petMapper.toPetDto(pet)).thenReturn(petDto);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.getPet(petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(petDto);
    }

    @Test
    void getPet_petDoesNotExist_returnsStatusNotFound() {
        // Arrange
        Integer petId = 1;
        when(clinicService.findPetById(petId)).thenReturn(null);
        when(petMapper.toPetDto(null)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.getPet(petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void listPets_petsExist_returnsListOfPetDtoWithStatusOk() {
        // Arrange
        List<Pet> pets = new ArrayList<>();
        pets.add(new Pet());
        List<PetDto> petDtos = new ArrayList<>();
        petDtos.add(new PetDto());
        when(clinicService.findAllPets()).thenReturn(pets);
        when(petMapper.toPetsDto(pets)).thenReturn(petDtos);

        // Act
        ResponseEntity<List<PetDto>> response = petRestControllerV1.listPets();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(petDtos);
    }

    @Test
    void listPets_noPetsExist_returnsStatusNotFound() {
        // Arrange
        List<Pet> pets = new ArrayList<>();
        List<PetDto> petDtos = new ArrayList<>();
        when(clinicService.findAllPets()).thenReturn(pets);
        when(petMapper.toPetsDto(pets)).thenReturn(petDtos);

        // Act
        ResponseEntity<List<PetDto>> response = petRestControllerV1.listPets();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void updatePet_petExists_updatesPetAndReturnsNoContent() {
        // Arrange
        Integer petId = 1;
        Pet currentPet = new Pet();
        PetDto petDto = new PetDto();
        petDto.setBirthDate(LocalDate.of(2020, 1, 1));
        petDto.setName("Buddy");
        PetType petType = new PetType();
        petDto.setType(null); // type DTO can be null or any object, here null for simplicity

        when(clinicService.findPetById(petId)).thenReturn(currentPet);
        when(petMapper.toPetType(petDto.getType())).thenReturn(petType);
        when(petMapper.toPetDto(currentPet)).thenReturn(petDto);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.updatePet(petId, petDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isEqualTo(petDto);

        assertThat(currentPet.getBirthDate()).isEqualTo(petDto.getBirthDate());
        assertThat(currentPet.getName()).isEqualTo(petDto.getName());
        assertThat(currentPet.getType()).isEqualTo(petType);

        verify(clinicService).savePet(currentPet);
    }

    @Test
    void updatePet_petDoesNotExist_returnsStatusNotFound() {
        // Arrange
        Integer petId = 1;
        PetDto petDto = new PetDto();
        when(clinicService.findPetById(petId)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.updatePet(petId, petDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void deletePet_petExists_deletesPetAndReturnsNoContent() {
        // Arrange
        Integer petId = 1;
        Pet pet = new Pet();
        when(clinicService.findPetById(petId)).thenReturn(pet);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.deletePet(petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(clinicService).deletePet(pet);
    }

    @Test
    void deletePet_petDoesNotExist_returnsStatusNotFound() {
        // Arrange
        Integer petId = 1;
        when(clinicService.findPetById(petId)).thenReturn(null);

        // Act
        ResponseEntity<PetDto> response = petRestControllerV1.deletePet(petId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
