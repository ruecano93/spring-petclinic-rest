package org.springframework.samples.petclinic.rest.controller.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.OwnerMapper;
import org.springframework.samples.petclinic.model.Owner;
import org.springframework.samples.petclinic.rest.dto.OwnerPageDto;
import org.springframework.samples.petclinic.service.ClinicService;

import java.util.Collections;
import java.util.List;

class OwnerRestControllerV2Test {

    @Mock
    ClinicService clinicService;

    @Mock
    OwnerMapper ownerMapper;

    @InjectMocks
    OwnerRestControllerV2 ownerRestControllerV2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listOwnersPage_nullPageAndSize_returnsOwnerPageDtoWithDefaults() {
        // Arrange
        String lastName = "Smith";
        Integer page = null;
        Integer size = null;
        int expectedPage = 0;
        int expectedSize = 20;
        PageRequest expectedPageRequest = PageRequest.of(expectedPage, expectedSize, Sort.by("id"));
        List<Owner> ownersList = List.of(new Owner());
        Page<Owner> ownersPage = new PageImpl<>(ownersList, expectedPageRequest, ownersList.size());
        OwnerPageDto ownerPageDto = new OwnerPageDto();

        when(clinicService.findOwners(eq(lastName), eq(expectedPageRequest))).thenReturn(ownersPage);
        when(ownerMapper.toOwnerPageDto(ownersPage)).thenReturn(ownerPageDto);

        // Act
        ResponseEntity<OwnerPageDto> response = ownerRestControllerV2.listOwnersPage(lastName, page, size);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ownerPageDto);
    }

    @Test
    void listOwnersPage_validPageAndSize_returnsOwnerPageDto() {
        // Arrange
        String lastName = "Johnson";
        Integer page = 2;
        Integer size = 10;
        PageRequest expectedPageRequest = PageRequest.of(page, size, Sort.by("id"));
        List<Owner> ownersList = List.of(new Owner(), new Owner());
        Page<Owner> ownersPage = new PageImpl<>(ownersList, expectedPageRequest, ownersList.size());
        OwnerPageDto ownerPageDto = new OwnerPageDto();

        when(clinicService.findOwners(eq(lastName), eq(expectedPageRequest))).thenReturn(ownersPage);
        when(ownerMapper.toOwnerPageDto(ownersPage)).thenReturn(ownerPageDto);

        // Act
        ResponseEntity<OwnerPageDto> response = ownerRestControllerV2.listOwnersPage(lastName, page, size);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(ownerPageDto);
    }

    @Test
    void listOwnersPage_emptyResult_returnsEmptyOwnerPageDto() {
        // Arrange
        String lastName = "NonExistentLastName";
        Integer page = null;
        Integer size = null;
        int expectedPage = 0;
        int expectedSize = 20;
        PageRequest expectedPageRequest = PageRequest.of(expectedPage, expectedSize, Sort.by("id"));
        Page<Owner> emptyOwnersPage = new PageImpl<>(Collections.emptyList(), expectedPageRequest, 0);
        OwnerPageDto emptyOwnerPageDto = new OwnerPageDto();

        when(clinicService.findOwners(eq(lastName), eq(expectedPageRequest))).thenReturn(emptyOwnersPage);
        when(ownerMapper.toOwnerPageDto(emptyOwnersPage)).thenReturn(emptyOwnerPageDto);

        // Act
        ResponseEntity<OwnerPageDto> response = ownerRestControllerV2.listOwnersPage(lastName, page, size);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(emptyOwnerPageDto);
    }
}
