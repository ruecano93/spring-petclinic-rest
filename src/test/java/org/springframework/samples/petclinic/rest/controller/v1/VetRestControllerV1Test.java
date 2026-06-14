package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.SpecialtyMapper;
import org.springframework.samples.petclinic.mapper.VetMapper;
import org.springframework.samples.petclinic.model.Specialty;
import org.springframework.samples.petclinic.model.Vet;
import org.springframework.samples.petclinic.rest.dto.VetDto;
import org.springframework.samples.petclinic.service.ClinicService;
import org.springframework.security.test.context.support.WithMockUser;

public class VetRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    VetMapper vetMapper;

    @Mock
    SpecialtyMapper specialtyMapper;

    @InjectMocks
    VetRestControllerV1 vetRestControllerV1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void listVets_whenVetsExist_returnsOkWithVetList() {
        // Arrange
        Vet vet = new Vet();
        vet.setId(1);
        List<Vet> vets = List.of(vet);
        VetDto vetDto = new VetDto();
        vetDto.setId(1);
        when(clinicService.findAllVets()).thenReturn(vets);
        when(vetMapper.toVetDtos(vets)).thenReturn(List.of(vetDto));

        // Act
        ResponseEntity<List<VetDto>> response = vetRestControllerV1.listVets();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(vetDto);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void listVets_whenNoVets_returnsNotFound() {
        // Arrange
        when(clinicService.findAllVets()).thenReturn(Collections.emptyList());
        when(vetMapper.toVetDtos(Collections.emptyList())).thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<VetDto>> response = vetRestControllerV1.listVets();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void getVet_whenVetExists_returnsOkWithVetDto() {
        // Arrange
        int vetId = 1;
        Vet vet = new Vet();
        vet.setId(vetId);
        VetDto vetDto = new VetDto();
        vetDto.setId(vetId);
        when(clinicService.findVetById(vetId)).thenReturn(vet);
        when(vetMapper.toVetDto(vet)).thenReturn(vetDto);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.getVet(vetId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(vetDto);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void getVet_whenVetNotFound_returnsNotFound() {
        // Arrange
        int vetId = 1;
        when(clinicService.findVetById(vetId)).thenReturn(null);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.getVet(vetId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void addVet_whenVetDtoValidWithSpecialties_returnsCreatedWithLocationHeader() {
        // Arrange
        VetDto vetDto = new VetDto();
        vetDto.setId(null);
        vetDto.setFirstName("John");
        vetDto.setLastName("Doe");
        Specialty specialty = new Specialty();
        specialty.setName("surgery");
        Set<Specialty> specialtiesSet = new HashSet<>();
        specialtiesSet.add(specialty);
        Vet vet = new Vet();
        vet.setId(1);
        vet.setFirstName("John");
        vet.setLastName("Doe");
        vet.addSpecialty(specialty);

        when(vetMapper.toVet(vetDto)).thenReturn(vet);
        when(clinicService.findSpecialtiesByNameIn(any())).thenReturn(List.of(specialty));
        doNothing().when(clinicService).saveVet(vet);
        VetDto returnedVetDto = new VetDto();
        returnedVetDto.setId(1);
        returnedVetDto.setFirstName("John");
        returnedVetDto.setLastName("Doe");
        when(vetMapper.toVetDto(vet)).thenReturn(returnedVetDto);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.addVet(vetDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/vets/1"));
        assertThat(response.getBody()).isEqualTo(returnedVetDto);
        verify(clinicService, times(1)).saveVet(vet);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void addVet_whenVetDtoValidWithoutSpecialties_returnsCreatedWithLocationHeader() {
        // Arrange
        VetDto vetDto = new VetDto();
        vetDto.setId(null);
        vetDto.setFirstName("Jane");
        vetDto.setLastName("Smith");
        Vet vet = new Vet();
        vet.setId(2);
        vet.setFirstName("Jane");
        vet.setLastName("Smith");

        when(vetMapper.toVet(vetDto)).thenReturn(vet);
        doNothing().when(clinicService).saveVet(vet);
        VetDto returnedVetDto = new VetDto();
        returnedVetDto.setId(2);
        returnedVetDto.setFirstName("Jane");
        returnedVetDto.setLastName("Smith");
        when(vetMapper.toVetDto(vet)).thenReturn(returnedVetDto);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.addVet(vetDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getHeaders().getLocation()).isEqualTo(URI.create("/api/vets/2"));
        assertThat(response.getBody()).isEqualTo(returnedVetDto);
        verify(clinicService, times(1)).saveVet(vet);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void updateVet_whenVetExists_updatesVetAndReturnsNoContent() {
        // Arrange
        int vetId = 1;
        VetDto vetDto = new VetDto();
        vetDto.setFirstName("UpdatedFirst");
        vetDto.setLastName("UpdatedLast");
        Specialty specialty = new Specialty();
        specialty.setName("dentistry");
        VetDto.SpecialtyDto specialtyDto = new VetDto.SpecialtyDto();
        specialtyDto.setName("dentistry");
        vetDto.setSpecialties(List.of(specialtyDto));

        Vet currentVet = new Vet();
        currentVet.setId(vetId);
        currentVet.setFirstName("OldFirst");
        currentVet.setLastName("OldLast");
        currentVet.addSpecialty(new Specialty("surgery"));

        when(clinicService.findVetById(vetId)).thenReturn(currentVet);
        when(specialtyMapper.toSpecialtys(vetDto.getSpecialties())).thenReturn(List.of(specialty));
        when(clinicService.findSpecialtiesByNameIn(any())).thenReturn(List.of(specialty));
        doNothing().when(clinicService).saveVet(currentVet);
        VetDto updatedVetDto = new VetDto();
        updatedVetDto.setFirstName("UpdatedFirst");
        updatedVetDto.setLastName("UpdatedLast");
        updatedVetDto.setSpecialties(List.of(specialtyDto));
        when(vetMapper.toVetDto(currentVet)).thenReturn(updatedVetDto);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.updateVet(vetId, vetDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        assertThat(currentVet.getFirstName()).isEqualTo("UpdatedFirst");
        assertThat(currentVet.getLastName()).isEqualTo("UpdatedLast");
        assertThat(currentVet.getSpecialties()).hasSize(1);
        assertThat(currentVet.getSpecialties().iterator().next().getName()).isEqualTo("dentistry");
        verify(clinicService, times(1)).saveVet(currentVet);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void updateVet_whenVetNotFound_returnsNotFound() {
        // Arrange
        int vetId = 1;
        VetDto vetDto = new VetDto();
        when(clinicService.findVetById(vetId)).thenReturn(null);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.updateVet(vetId, vetDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void deleteVet_whenVetExists_deletesVetAndReturnsNoContent() {
        // Arrange
        int vetId = 1;
        Vet vet = new Vet();
        vet.setId(vetId);
        when(clinicService.findVetById(vetId)).thenReturn(vet);
        doNothing().when(clinicService).deleteVet(vet);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.deleteVet(vetId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(clinicService, times(1)).deleteVet(vet);
    }

    @Test
    @WithMockUser(roles = {"VET_ADMIN"})
    void deleteVet_whenVetNotFound_returnsNotFound() {
        // Arrange
        int vetId = 1;
        when(clinicService.findVetById(vetId)).thenReturn(null);

        // Act
        ResponseEntity<VetDto> response = vetRestControllerV1.deleteVet(vetId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }
}
