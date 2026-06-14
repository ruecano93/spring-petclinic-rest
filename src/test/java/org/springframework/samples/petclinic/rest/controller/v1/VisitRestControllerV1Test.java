package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.VisitMapper;
import org.springframework.samples.petclinic.model.Visit;
import org.springframework.samples.petclinic.rest.dto.VisitDto;
import org.springframework.samples.petclinic.rest.dto.VisitFieldsDto;
import org.springframework.samples.petclinic.service.ClinicService;

class VisitRestControllerV1Test {

    @Mock
    ClinicService clinicService;

    @Mock
    VisitMapper visitMapper;

    @InjectMocks
    VisitRestControllerV1 visitRestControllerV1;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listVisits_whenVisitsExist_returnsOkWithVisitDtoList() {
        // Arrange
        Visit visit1 = new Visit();
        visit1.setId(1);
        Visit visit2 = new Visit();
        visit2.setId(2);
        List<Visit> visits = List.of(visit1, visit2);
        when(clinicService.findAllVisits()).thenReturn(visits);

        VisitDto visitDto1 = new VisitDto();
        visitDto1.setId(1);
        VisitDto visitDto2 = new VisitDto();
        visitDto2.setId(2);
        List<VisitDto> visitDtos = List.of(visitDto1, visitDto2);
        when(visitMapper.toVisitsDto(visits)).thenReturn(visitDtos);

        // Act
        ResponseEntity<List<VisitDto>> response = visitRestControllerV1.listVisits();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(visitDtos);
    }

    @Test
    void listVisits_whenNoVisitsExist_returnsNotFound() {
        // Arrange
        when(clinicService.findAllVisits()).thenReturn(new ArrayList<>());

        // Act
        ResponseEntity<List<VisitDto>> response = visitRestControllerV1.listVisits();

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void getVisit_whenVisitExists_returnsOkWithVisitDto() {
        // Arrange
        Integer visitId = 1;
        Visit visit = new Visit();
        visit.setId(visitId);
        when(clinicService.findVisitById(visitId)).thenReturn(visit);

        VisitDto visitDto = new VisitDto();
        visitDto.setId(visitId);
        when(visitMapper.toVisitDto(visit)).thenReturn(visitDto);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.getVisit(visitId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(visitDto);
    }

    @Test
    void getVisit_whenVisitDoesNotExist_returnsNotFound() {
        // Arrange
        Integer visitId = 1;
        when(clinicService.findVisitById(visitId)).thenReturn(null);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.getVisit(visitId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
    }

    @Test
    void addVisit_whenValidVisitDto_returnsCreatedWithLocationHeader() {
        // Arrange
        VisitDto inputDto = new VisitDto();
        Visit visit = new Visit();
        visit.setId(1);
        VisitDto outputDto = new VisitDto();
        outputDto.setId(1);

        when(visitMapper.toVisit(inputDto)).thenReturn(visit);
        doAnswer(invocation -> {
            visit.setId(1);
            return null;
        }).when(clinicService).saveVisit(visit);
        when(visitMapper.toVisitDto(visit)).thenReturn(outputDto);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.addVisit(inputDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(outputDto);
        HttpHeaders headers = response.getHeaders();
        assertThat(headers.getLocation()).isEqualTo(URI.create("/api/visits/1"));
    }

    @Test
    void updateVisit_whenVisitExists_updatesVisitAndReturnsNoContent() {
        // Arrange
        Integer visitId = 1;
        Visit currentVisit = new Visit();
        currentVisit.setId(visitId);
        when(clinicService.findVisitById(visitId)).thenReturn(currentVisit);

        VisitFieldsDto visitDto = new VisitFieldsDto();
        LocalDate date = LocalDate.of(2024, 6, 1);
        visitDto.setDate(date);
        visitDto.setDescription("Updated description");

        VisitDto updatedDto = new VisitDto();
        updatedDto.setId(visitId);
        when(visitMapper.toVisitDto(currentVisit)).thenReturn(updatedDto);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.updateVisit(visitId, visitDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isEqualTo(updatedDto);
        assertThat(currentVisit.getDate()).isEqualTo(date);
        assertThat(currentVisit.getDescription()).isEqualTo("Updated description");
        verify(clinicService).saveVisit(currentVisit);
    }

    @Test
    void updateVisit_whenVisitDoesNotExist_returnsNotFound() {
        // Arrange
        Integer visitId = 1;
        when(clinicService.findVisitById(visitId)).thenReturn(null);
        VisitFieldsDto visitDto = new VisitFieldsDto();

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.updateVisit(visitId, visitDto);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNull();
        verify(clinicService, never()).saveVisit(any());
    }

    @Test
    void deleteVisit_whenVisitExists_deletesVisitAndReturnsNoContent() {
        // Arrange
        Integer visitId = 1;
        Visit visit = new Visit();
        visit.setId(visitId);
        when(clinicService.findVisitById(visitId)).thenReturn(visit);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.deleteVisit(visitId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(clinicService).deleteVisit(visit);
    }

    @Test
    void deleteVisit_whenVisitDoesNotExist_returnsNotFound() {
        // Arrange
        Integer visitId = 1;
        when(clinicService.findVisitById(visitId)).thenReturn(null);

        // Act
        ResponseEntity<VisitDto> response = visitRestControllerV1.deleteVisit(visitId);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(clinicService, never()).deleteVisit(any());
    }

}
