package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.PetTypeMapper;
import org.springframework.samples.petclinic.model.PetType;
import org.springframework.samples.petclinic.rest.dto.PetTypeDto;
import org.springframework.samples.petclinic.rest.dto.PetTypeFieldsDto;
import org.springframework.samples.petclinic.service.ClinicService;

class PetTypeRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    PetTypeMapper petTypeMapper;

    @InjectMocks
    PetTypeRestControllerV1 petTypeRestControllerV1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listPetTypes_whenPetTypesExist_returnsOkWithPetTypeDtoList() {
        // Arrange
        List<PetType> petTypes = new ArrayList<>();
        PetType petType = new PetType();
        petType.setId(1);
        petType.setName("Dog");
        petTypes.add(petType);

        List<PetTypeDto> petTypeDtos = new ArrayList<>();
        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(1);
        petTypeDto.setName("Dog");
        petTypeDtos.add(petTypeDto);

        when(clinicService.findAllPetTypes()).thenReturn(petTypes);
        when(petTypeMapper.toPetTypeDtos(petTypes)).thenReturn(petTypeDtos);

        // Act
        ResponseEntity<List<PetTypeDto>> response = petTypeRestControllerV1.listPetTypes();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(petTypeDtos);
    }

    @Test
    void listPetTypes_whenNoPetTypes_returnsNotFound() {
        // Arrange
        when(clinicService.findAllPetTypes()).thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<List<PetTypeDto>> response = petTypeRestControllerV1.listPetTypes();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getPetType_whenPetTypeExists_returnsOkWithPetTypeDto() {
        // Arrange
        Integer id = 1;
        PetType petType = new PetType();
        petType.setId(id);
        petType.setName("Cat");
        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(id);
        petTypeDto.setName("Cat");

        when(clinicService.findPetTypeById(id)).thenReturn(petType);
        when(petTypeMapper.toPetTypeDto(petType)).thenReturn(petTypeDto);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.getPetType(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(petTypeDto);
    }

    @Test
    void getPetType_whenPetTypeNotFound_returnsNotFound() {
        // Arrange
        Integer id = 1;
        when(clinicService.findPetTypeById(id)).thenReturn(null);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.getPetType(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void addPetType_whenValidPetTypeFieldsDto_returnsCreatedWithLocationHeader() {
        // Arrange
        PetTypeFieldsDto petTypeFieldsDto = new PetTypeFieldsDto();
        petTypeFieldsDto.setName("Bird");

        PetType petType = new PetType();
        petType.setId(5);
        petType.setName("Bird");

        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(5);
        petTypeDto.setName("Bird");

        when(petTypeMapper.toPetType(petTypeFieldsDto)).thenReturn(petType);
        doNothing().when(clinicService).savePetType(petType);
        when(petTypeMapper.toPetTypeDto(petType)).thenReturn(petTypeDto);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.addPetType(petTypeFieldsDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/pettypes/5"));
        assertThat(response.getBody()).isEqualTo(petTypeDto);
    }

    @Test
    void updatePetType_whenPetTypeExists_updatesAndReturnsNoContent() {
        // Arrange
        Integer id = 3;
        PetType currentPetType = new PetType();
        currentPetType.setId(id);
        currentPetType.setName("Fish");

        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(id);
        petTypeDto.setName("Goldfish");

        PetTypeDto updatedPetTypeDto = new PetTypeDto();
        updatedPetTypeDto.setId(id);
        updatedPetTypeDto.setName("Goldfish");

        when(clinicService.findPetTypeById(id)).thenReturn(currentPetType);
        doNothing().when(clinicService).savePetType(currentPetType);
        when(petTypeMapper.toPetTypeDto(currentPetType)).thenReturn(updatedPetTypeDto);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.updatePetType(id, petTypeDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isEqualTo(updatedPetTypeDto);
        assertThat(currentPetType.getName()).isEqualTo("Goldfish");
    }

    @Test
    void updatePetType_whenPetTypeNotFound_returnsNotFound() {
        // Arrange
        Integer id = 7;
        PetTypeDto petTypeDto = new PetTypeDto();
        petTypeDto.setId(id);
        petTypeDto.setName("Hamster");

        when(clinicService.findPetTypeById(id)).thenReturn(null);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.updatePetType(id, petTypeDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void deletePetType_whenPetTypeExists_deletesAndReturnsNoContent() {
        // Arrange
        Integer id = 9;
        PetType petType = new PetType();
        petType.setId(id);
        petType.setName("Rabbit");

        when(clinicService.findPetTypeById(id)).thenReturn(petType);
        doNothing().when(clinicService).deletePetType(petType);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.deletePetType(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(clinicService).deletePetType(petType);
    }

    @Test
    void deletePetType_whenPetTypeNotFound_returnsNotFound() {
        // Arrange
        Integer id = 11;
        when(clinicService.findPetTypeById(id)).thenReturn(null);

        // Act
        ResponseEntity<PetTypeDto> response = petTypeRestControllerV1.deletePetType(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

}
