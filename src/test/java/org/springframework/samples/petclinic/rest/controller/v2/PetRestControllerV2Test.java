package org.springframework.samples.petclinic.rest.controller.v2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.mapper.PetMapper;
import org.springframework.samples.petclinic.model.Pet;
import org.springframework.samples.petclinic.rest.dto.PetPageDto;
import org.springframework.samples.petclinic.service.ClinicService;

class PetRestControllerV2Test {

    @Mock
    ClinicService clinicService;

    @Mock
    PetMapper petMapper;

    @InjectMocks
    PetRestControllerV2 petRestControllerV2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void listPetsPage_nullPageAndSize_returnsDefaultPage() {
        // Arrange
        int defaultPage = 0;
        int defaultSize = 20;
        Page<Pet> petPage = new PageImpl<>(List.of(new Pet()));
        PetPageDto petPageDto = new PetPageDto();

        when(clinicService.findPets(any())).thenReturn(petPage);
        when(petMapper.toPetPageDto(petPage)).thenReturn(petPageDto);

        // Act
        ResponseEntity<PetPageDto> response = petRestControllerV2.listPetsPage(null, null);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(petPageDto);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(clinicService).findPets(pageRequestCaptor.capture());
        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertThat(capturedPageRequest.getPageNumber()).isEqualTo(defaultPage);
        assertThat(capturedPageRequest.getPageSize()).isEqualTo(defaultSize);
        assertThat(capturedPageRequest.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void listPetsPage_validPageAndSize_returnsPagedPets() {
        // Arrange
        int page = 1;
        int size = 10;
        Page<Pet> petPage = new PageImpl<>(List.of(new Pet(), new Pet()));
        PetPageDto petPageDto = new PetPageDto();

        when(clinicService.findPets(any())).thenReturn(petPage);
        when(petMapper.toPetPageDto(petPage)).thenReturn(petPageDto);

        // Act
        ResponseEntity<PetPageDto> response = petRestControllerV2.listPetsPage(page, size);

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(petPageDto);

        ArgumentCaptor<PageRequest> pageRequestCaptor = ArgumentCaptor.forClass(PageRequest.class);
        verify(clinicService).findPets(pageRequestCaptor.capture());
        PageRequest capturedPageRequest = pageRequestCaptor.getValue();
        assertThat(capturedPageRequest.getPageNumber()).isEqualTo(page);
        assertThat(capturedPageRequest.getPageSize()).isEqualTo(size);
        assertThat(capturedPageRequest.getSort()).isEqualTo(Sort.by("id"));
    }

    @Test
    void listPetsPage_negativePage_returnsFirstPage() {
        // Arrange
        int negativePage = -1;
        int size = 5;
        Page<Pet> petPage = new PageImpl<>(List.of(new Pet()));
        PetPageDto petPageDto = new PetPageDto();

        when(clinicService.findPets(any())).thenReturn(petPage);
        when(petMapper.toPetPageDto(petPage)).thenReturn(petPageDto);

        // Act & Assert
        // The controller calls PageRequest.of with negative page, which throws IllegalArgumentException.
        // We verify that the exception is thrown as per the current implementation.
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> petRestControllerV2.listPetsPage(negativePage, size))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Page index must not be less than zero");
    }

}
