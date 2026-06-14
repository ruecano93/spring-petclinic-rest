package org.springframework.samples.petclinic.rest.advice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.samples.petclinic.rest.dto.ValidationMessageDto;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.net.URI;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ExceptionControllerAdviceTest {

    private ExceptionControllerAdvice exceptionControllerAdvice;

    private HttpServletRequest request;

    private static final String METHOD = "POST";
    private static final String REQUEST_URI = "/api/test";
    private static final String REQUEST_URL = "http://localhost/api/test";

    @BeforeEach
    void setUp() {
        exceptionControllerAdvice = new ExceptionControllerAdvice();
        request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(METHOD);
        when(request.getRequestURI()).thenReturn(REQUEST_URI);
        when(request.getRequestURL()).thenReturn(new StringBuffer(REQUEST_URL));
    }

    @Test
    void handleGeneralException_validException_returnsInternalServerErrorWithProblemDetail() {
        // Arrange
        Exception exception = new Exception("General error");

        // Act
        ResponseEntity<ProblemDetail> response = exceptionControllerAdvice.handleGeneralException(exception, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.getTitle()).isEqualTo(exception.getClass().getSimpleName());
        assertThat(detail.getType()).isEqualTo(URI.create(REQUEST_URL));
        assertThat(detail.getDetail()).isEqualTo("An unexpected error occurred while processing your request");
        assertThat(detail.getProperties()).containsKey("timestamp");
        assertThat(detail.getProperties()).containsEntry("schemaValidationErrors", List.<ValidationMessageDto>of());
    }

    @Test
    void handleDataIntegrityViolationException_validException_returnsNotFoundWithProblemDetail() {
        // Arrange
        String message = "Constraint violation";
        DataIntegrityViolationException exception = new DataIntegrityViolationException(message);

        // Act
        ResponseEntity<ProblemDetail> response = exceptionControllerAdvice.handleDataIntegrityViolationException(exception, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.getTitle()).isEqualTo(exception.getClass().getSimpleName());
        assertThat(detail.getType()).isEqualTo(URI.create(REQUEST_URL));
        assertThat(detail.getDetail()).isEqualTo("The requested resource could not be processed due to a data constraint violation");
        assertThat(detail.getProperties()).containsKey("timestamp");
        assertThat(detail.getProperties()).containsEntry("schemaValidationErrors", List.<ValidationMessageDto>of());
    }

    @Test
    void handleMethodArgumentNotValidException_withFieldErrors_returnsBadRequestWithValidationErrors() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(true);

        FieldError fieldError1 = new FieldError("objectName", "field1", "rejectedValue1", false, null, null, "must not be null");
        FieldError fieldError2 = new FieldError("objectName", "field2", null, false, null, null, "size must be between 1 and 10");
        List<FieldError> fieldErrors = List.of(fieldError1, fieldError2);
        when(bindingResult.getFieldErrors()).thenReturn(fieldErrors);

        // Act
        ResponseEntity<ProblemDetail> response = exceptionControllerAdvice.handleMethodArgumentNotValidException(exception, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.getTitle()).isEqualTo(exception.getClass().getSimpleName());
        assertThat(detail.getType()).isEqualTo(URI.create(REQUEST_URL));
        assertThat(detail.getDetail()).isEqualTo("The request contains invalid or missing parameters");
        assertThat(detail.getProperties()).containsKey("timestamp");
        assertThat(detail.getProperties()).containsKey("schemaValidationErrors");
        Object errorsObj = detail.getProperties().get("schemaValidationErrors");
        assertThat(errorsObj).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<ValidationMessageDto> validationErrors = (List<ValidationMessageDto>) errorsObj;
        assertThat(validationErrors).hasSize(2);
        ValidationMessageDto v1 = validationErrors.get(0);
        assertThat(v1.getMessage()).isEqualTo("Field 'field1' must not be null (rejected value: rejectedValue1)");
        assertThat(v1.getAdditionalProperties()).containsEntry("field", "field1");
        assertThat(v1.getAdditionalProperties()).containsEntry("rejectedValue", "rejectedValue1");
        assertThat(v1.getAdditionalProperties()).containsEntry("defaultMessage", "must not be null");
        ValidationMessageDto v2 = validationErrors.get(1);
        assertThat(v2.getMessage()).isEqualTo("Field 'field2' size must be between 1 and 10 (rejected value: null)");
        assertThat(v2.getAdditionalProperties()).containsEntry("field", "field2");
        assertThat(v2.getAdditionalProperties()).containsEntry("rejectedValue", "null");
        assertThat(v2.getAdditionalProperties()).containsEntry("defaultMessage", "size must be between 1 and 10");
    }

    @Test
    void handleMethodArgumentNotValidException_withoutFieldErrors_returnsBadRequestWithEmptyValidationErrors() {
        // Arrange
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException exception = mock(MethodArgumentNotValidException.class);
        when(exception.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.hasErrors()).thenReturn(false);

        // Act
        ResponseEntity<ProblemDetail> response = exceptionControllerAdvice.handleMethodArgumentNotValidException(exception, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ProblemDetail detail = response.getBody();
        assertThat(detail).isNotNull();
        assertThat(detail.getTitle()).isEqualTo(exception.getClass().getSimpleName());
        assertThat(detail.getType()).isEqualTo(URI.create(REQUEST_URL));
        assertThat(detail.getDetail()).isEqualTo("The request contains invalid or missing parameters");
        assertThat(detail.getProperties()).containsKey("timestamp");
        Object errorsObj = detail.getProperties().get("schemaValidationErrors");
        assertThat(errorsObj).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<ValidationMessageDto> validationErrors = (List<ValidationMessageDto>) errorsObj;
        assertThat(validationErrors).isEmpty();
    }

}
