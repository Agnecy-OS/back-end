package dev.eyadsharkawy.agency_os_api.global.demo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/public/demo")
@RequiredArgsConstructor
@Tag(name = "00. Demo", description = "Showcase demo environment operations")
public class DemoResetController {

  private static final String KEY_SUCCESS = "success";
  private static final String KEY_MESSAGE = "message";
  private static final String KEY_RESET_AT = "resetAt";

  private final DemoDataResetService demoDataResetService;

  @Value("${app.demo.reset.enabled:true}")
  private boolean resetEnabled;

  private Instant lastResetTime = Instant.EPOCH;

  @PostMapping("/reset")
  @Operation(
      summary = "Reset demo showcase dataset",
      description =
          "Wipes and restores the standard demo showcase workspace and tenant data with fresh relative timestamps.")
  public ResponseEntity<Map<String, Object>> resetDemo() {
    if (!resetEnabled) {
      return ResponseEntity.badRequest()
          .body(
              Map.of(
                  KEY_SUCCESS,
                  false,
                  KEY_MESSAGE,
                  "Demo showcase reset is currently disabled on this instance."));
    }

    synchronized (this) {
      Instant now = Instant.now();
      if (lastResetTime.plusSeconds(5).isAfter(now)) {
        return ResponseEntity.ok(
            Map.of(
                KEY_SUCCESS,
                true,
                KEY_MESSAGE,
                "Demo data was reset moments ago. Dataset is already pristine.",
                KEY_RESET_AT,
                lastResetTime.toString()));
      }

      log.info("Public demo reset requested via API endpoint.");
      demoDataResetService.resetDemoData();
      lastResetTime = now;

      return ResponseEntity.ok(
          Map.of(
              KEY_SUCCESS,
              true,
              KEY_MESSAGE,
              "Demo showcase data successfully reset with fresh timestamps and standard records.",
              KEY_RESET_AT,
              lastResetTime.toString()));
    }
  }
}
