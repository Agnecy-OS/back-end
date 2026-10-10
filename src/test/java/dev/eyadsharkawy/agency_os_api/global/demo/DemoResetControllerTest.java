package dev.eyadsharkawy.agency_os_api.global.demo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DemoResetControllerTest {

  @Mock private DemoDataResetService demoDataResetService;

  @InjectMocks private DemoResetController controller;

  @Test
  @DisplayName("resetDemo should execute reset and return success when enabled")
  void resetDemo_Success() {
    ReflectionTestUtils.setField(controller, "resetEnabled", true);

    ResponseEntity<Map<String, Object>> response = controller.resetDemo();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).containsEntry("success", true);
    verify(demoDataResetService, times(1)).resetDemoData();
  }

  @Test
  @DisplayName("resetDemo should throttle consecutive calls within 5 seconds")
  void resetDemo_Throttled() {
    ReflectionTestUtils.setField(controller, "resetEnabled", true);

    ResponseEntity<Map<String, Object>> first = controller.resetDemo();
    ResponseEntity<Map<String, Object>> second = controller.resetDemo();

    assertThat(first.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(second.getBody().get("message").toString()).contains("moments ago");
    verify(demoDataResetService, times(1)).resetDemoData();
  }

  @Test
  @DisplayName("resetDemo should return bad request when reset is disabled")
  void resetDemo_Disabled() {
    ReflectionTestUtils.setField(controller, "resetEnabled", false);

    ResponseEntity<Map<String, Object>> response = controller.resetDemo();

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).containsEntry("success", false);
    verify(demoDataResetService, never()).resetDemoData();
  }
}
