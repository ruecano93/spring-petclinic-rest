package org.springframework.samples.petclinic.rest.controller.v1;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RootRestControllerV1Test {

	private RootRestControllerV1 controller;

	private HttpServletResponse response;

	@BeforeEach
	void setUp() {
		controller = new RootRestControllerV1();
		response = Mockito.mock(HttpServletResponse.class);
	}

	@Test
	void redirectToSwagger_validContextPath_redirectsToSwaggerUi() throws IOException {
		// Arrange
		setServletContextPath("/app");

		// Act
		controller.redirectToSwagger(response);

		// Assert
		verify(response).sendRedirect("/app/swagger-ui/index.html");
	}

	@Test
	void redirectToSwagger_emptyContextPath_redirectsToSwaggerUiRoot() throws IOException {
		// Arrange
		setServletContextPath("");

		// Act
		controller.redirectToSwagger(response);

		// Assert
		verify(response).sendRedirect("/swagger-ui/index.html");
	}

	private void setServletContextPath(String path) {
		try {
			var field = RootRestControllerV1.class.getDeclaredField("servletContextPath");
			field.setAccessible(true);
			field.set(controller, path);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException(e);
		}
	}
}
