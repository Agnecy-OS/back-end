package dev.eyadsharkawy.agency_os_api.global.demo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DemoDataResetScheduler {

  private final DemoDataResetService demoDataResetService;

  @Value("${app.demo.reset.enabled:true}")
  private boolean resetEnabled;

  @Value("${app.demo.reset.on-startup:true}")
  private boolean resetOnStartup;

  @Order(100)
  @EventListener(ApplicationReadyEvent.class)
  public void onApplicationReady() {
    if (resetEnabled && resetOnStartup) {
      log.info("Application ready: performing initial demo showcase workspace reset...");
      try {
        demoDataResetService.resetDemoData();
      } catch (Exception ex) {
        log.error(
            "Initial demo showcase reset encountered an error on startup: {}", ex.getMessage());
      }
    } else {
      log.info(
          "Startup demo showcase reset skipped (enabled={}, onStartup={})",
          resetEnabled,
          resetOnStartup);
    }
  }

  @Scheduled(cron = "${app.demo.reset.cron:0 0 */6 * * *}")
  public void scheduledReset() {
    if (!resetEnabled) {
      log.debug("Demo showcase reset is disabled. Skipping scheduled execution.");
      return;
    }
    log.info("Executing scheduled periodic reset of demo showcase workspace...");
    try {
      demoDataResetService.resetDemoData();
    } catch (Exception ex) {
      log.error("Scheduled demo showcase reset encountered an error: {}", ex.getMessage());
    }
  }
}
