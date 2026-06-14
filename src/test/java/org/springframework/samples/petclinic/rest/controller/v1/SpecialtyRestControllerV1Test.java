package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.SpecialtyMapper;
import org.springframework.samples.petclinic.model.Specialty;
import org.springframework.samples.petclinic.rest.dto.SpecialtyDto;
import org.springframework.samples.petclinic.service.ClinicService;

class SpecialtyRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    SpecialtyMapper specialtyMapper;

    @InjectMocks
    SpecialtyRestControllerV1 specialtyRestControllerV1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listSpecialties_whenSpecialtiesExist_returnsOkWithList() {
        // Arrange
        List<Specialty> specialties = new ArrayList<>();
        specialties.add(new Specialty());
        List<SpecialtyDto> specialtyDtos = new ArrayList<>();
        specialtyDtos.add(new SpecialtyDto());

        when(clinicService.findAllSpecialties()).thenReturn(specialties);
        when(specialtyMapper.toSpecialtyDtos(specialties)).thenReturn(specialtyDtos);

        // Act
        ResponseEntity<List<SpecialtyDto>> response = specialtyRestControllerV1.listSpecialties();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(specialtyDtos);
    }

    @Test
    void listSpecialties_whenNoSpecialties_returnsNotFound() {
        // Arrange
        List<Specialty> specialties = new ArrayList<>();
        List<SpecialtyDto> specialtyDtos = new ArrayList<>();

        when(clinicService.findAllSpecialties()).thenReturn(specialties);
        when(specialtyMapper.toSpecialtyDtos(specialties)).thenReturn(specialtyDtos);

        // Act
        ResponseEntity<List<SpecialtyDto>> response = specialtyRestControllerV1.listSpecialties();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getSpecialty_whenSpecialtyExists_returnsOkWithSpecialtyDto() {
        // Arrange
        Integer id = 1;
        Specialty specialty = new Specialty();
        SpecialtyDto specialtyDto = new SpecialtyDto();

        when(clinicService.findSpecialtyById(id)).thenReturn(specialty);
        when(specialtyMapper.toSpecialtyDto(specialty)).thenReturn(specialtyDto);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.getSpecialty(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(specialtyDto);
    }

    @Test
    void getSpecialty_whenSpecialtyNotFound_returnsNotFound() {
        // Arrange
        Integer id = 1;

        when(clinicService.findSpecialtyById(id)).thenReturn(null);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.getSpecialty(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void addSpecialty_whenValidSpecialtyDto_returnsCreatedWithLocationHeader() {
        // Arrange
        SpecialtyDto dto = new SpecialtyDto();
        Specialty specialty = new Specialty();
        specialty.setId(1);

        when(specialtyMapper.toSpecialty(dto)).thenReturn(specialty);
        doAnswer(invocation -> {
            // simulate saveSpecialty setting id if needed
            return null;
        }).when(clinicService).saveSpecialty(specialty);
        when(specialtyMapper.toSpecialtyDto(specialty)).thenReturn(dto);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.addSpecialty(dto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/specialties/1"));
        assertThat(response.getBody()).isEqualTo(dto);
    }

    @Test
    void updateSpecialty_whenSpecialtyExists_updatesAndReturnsNoContent() {
        // Arrange
        Integer id = 1;
        Specialty currentSpecialty = new Specialty();
        currentSpecialty.setId(id);
        SpecialtyDto specialtyDto = new SpecialtyDto();
        specialtyDto.setName("NewName");
        SpecialtyDto updatedDto = new SpecialtyDto();

        when(clinicService.findSpecialtyById(id)).thenReturn(currentSpecialty);
        when(specialtyMapper.toSpecialtyDto(currentSpecialty)).thenReturn(updatedDto);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.updateSpecialty(id, specialtyDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(currentSpecialty.getName()).isEqualTo("NewName");
        assertThat(response.getBody()).isEqualTo(updatedDto);
        verify(clinicService).saveSpecialty(currentSpecialty);
    }

    @Test
    void updateSpecialty_whenSpecialtyNotFound_returnsNotFound() {
        // Arrange
        Integer id = 1;
        SpecialtyDto specialtyDto = new SpecialtyDto();

        when(clinicService.findSpecialtyById(id)).thenReturn(null);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.updateSpecialty(id, specialtyDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(clinicService, never()).saveSpecialty(any());
    }

    @Test
    void deleteSpecialty_whenSpecialtyExists_deletesAndReturnsNoContent() {
        // Arrange
        Integer id = 1;
        Specialty specialty = new Specialty();

        when(clinicService.findSpecialtyById(id)).thenReturn(specialty);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.deleteSpecialty(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(clinicService).deleteSpecialty(specialty);
    }

    @Test
    void deleteSpecialty_whenSpecialtyNotFound_returnsNotFound() {
        // Arrange
        Integer id = 1;

        when(clinicService.findSpecialtyById(id)).thenReturn(null);

        // Act
        ResponseEntity<SpecialtyDto> response = specialtyRestControllerV1.deleteSpecialty(id);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(clinicService, never()).deleteSpecialty(any());
    }

}
