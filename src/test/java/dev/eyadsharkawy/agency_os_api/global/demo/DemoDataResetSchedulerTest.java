package dev.eyadsharkawy.agency_os_api.global.demo;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class DemoDataResetSchedulerTest {

  @Mock private DemoDataResetService demoDataResetService;

  @InjectMocks private DemoDataResetScheduler scheduler;

  @Test
  @DisplayName("onApplicationReady should trigger reset when enabled and onStartup is true")
  void onApplicationReady_Enabled_TriggersReset() {
    ReflectionTestUtils.setField(scheduler, "resetEnabled", true);
    ReflectionTestUtils.setField(scheduler, "resetOnStartup", true);

    scheduler.onApplicationReady();

    verify(demoDataResetService, times(1)).resetDemoData();
  }

  @Test
  @DisplayName("onApplicationReady should skip reset when disabled or onStartup is false")
  void onApplicationReady_Disabled_SkipsReset() {
    ReflectionTestUtils.setField(scheduler, "resetEnabled", false);
    ReflectionTestUtils.setField(scheduler, "resetOnStartup", true);

    scheduler.onApplicationReady();

    verify(demoDataResetService, never()).resetDemoData();
  }

  @Test
  @DisplayName("scheduledReset should trigger reset when enabled")
  void scheduledReset_Enabled_TriggersReset() {
    ReflectionTestUtils.setField(scheduler, "resetEnabled", true);

    scheduler.scheduledReset();

    verify(demoDataResetService, times(1)).resetDemoData();
  }

  @Test
  @DisplayName("scheduledReset should skip reset when disabled")
  void scheduledReset_Disabled_SkipsReset() {
    ReflectionTestUtils.setField(scheduler, "resetEnabled", false);

    scheduler.scheduledReset();

    verify(demoDataResetService, never()).resetDemoData();
  }
}
