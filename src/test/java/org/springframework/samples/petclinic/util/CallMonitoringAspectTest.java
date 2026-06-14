package org.springframework.samples.petclinic.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

class CallMonitoringAspectTest {

    private CallMonitoringAspect aspect;

    @BeforeEach
    void setUp() {
        aspect = new CallMonitoringAspect();
    }

    @Test
    void invoke_enabled_shouldMonitorCallAndReturnResult() throws Throwable {
        // Arrange
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.toShortString()).thenReturn("shortString");
        String expectedResult = "result";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // Act
        Object result = aspect.invoke(joinPoint);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
        assertThat(aspect.getCallCount()).isEqualTo(1);
        assertThat(aspect.getCallTime()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void invoke_disabled_shouldReturnResultWithoutMonitoring() throws Throwable {
        // Arrange
        aspect.setEnabled(false);
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        String expectedResult = "result";
        when(joinPoint.proceed()).thenReturn(expectedResult);

        // Act
        Object result = aspect.invoke(joinPoint);

        // Assert
        assertThat(result).isEqualTo(expectedResult);
        assertThat(aspect.getCallCount()).isEqualTo(0);
        assertThat(aspect.getCallTime()).isEqualTo(0);
    }

    @Test
    void reset_shouldSetCallCountAndAccumulatedCallTimeToZero() {
        // Arrange
        aspect.setEnabled(true);
        // simulate some calls
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        try {
            when(joinPoint.toShortString()).thenReturn("shortString");
            when(joinPoint.proceed()).thenReturn("result");
            aspect.invoke(joinPoint);
            aspect.invoke(joinPoint);
        } catch (Throwable e) {
            // ignore for setup
        }
        assertThat(aspect.getCallCount()).isGreaterThan(0);
        assertThat(aspect.getCallTime()).isGreaterThanOrEqualTo(0);

        // Act
        aspect.reset();

        // Assert
        assertThat(aspect.getCallCount()).isEqualTo(0);
        assertThat(aspect.getCallTime()).isEqualTo(0);
    }

    @Test
    void getCallTime_withCalls_shouldReturnAverageCallTime() {
        // Arrange
        // Use reflection to set private fields for testing
        int callCount = 5;
        long accumulatedCallTime = 1000L;
        setField(aspect, "callCount", callCount);
        setField(aspect, "accumulatedCallTime", accumulatedCallTime);

        // Act
        long average = aspect.getCallTime();

        // Assert
        assertThat(average).isEqualTo(accumulatedCallTime / callCount);
    }

    @Test
    void getCallTime_withoutCalls_shouldReturnZero() {
        // Arrange
        setField(aspect, "callCount", 0);
        setField(aspect, "accumulatedCallTime", 1000L);

        // Act
        long average = aspect.getCallTime();

        // Assert
        assertThat(average).isEqualTo(0);
    }

    @Test
    void setEnabled_shouldChangeEnabledFlag() {
        // Arrange
        aspect.setEnabled(true);
        assertThat(aspect.isEnabled()).isTrue();

        // Act
        aspect.setEnabled(false);

        // Assert
        assertThat(aspect.isEnabled()).isFalse();

        // Act
        aspect.setEnabled(true);

        // Assert
        assertThat(aspect.isEnabled()).isTrue();
    }

    @Test
    void isEnabled_shouldReturnCurrentEnabledState() {
        // Arrange
        aspect.setEnabled(true);

        // Act & Assert
        assertThat(aspect.isEnabled()).isTrue();

        // Arrange
        aspect.setEnabled(false);

        // Act & Assert
        assertThat(aspect.isEnabled()).isFalse();
    }

    private void setField(CallMonitoringAspect aspect, String fieldName, Object value) {
        try {
            var field = CallMonitoringAspect.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(aspect, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
