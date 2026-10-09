package dev.eyadsharkawy.agency_os_api.global.demo;

import dev.eyadsharkawy.agency_os_api.global.workspace.service.TenantSchemaProvisioningService;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DemoDataResetService {

  public static final String DEMO_TENANT_ID = "tenant_apex_digital_demo";
  private static final String SEED_SCRIPT_PATH = "classpath:db/seed/demo-showcase-data.sql";

  private final DataSource dataSource;
  private final TenantSchemaProvisioningService tenantSchemaProvisioningService;
  private final ResourceLoader resourceLoader;

  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public synchronized void resetDemoData() {
    log.info("Initiating demo showcase workspace reset for tenant [{}]...", DEMO_TENANT_ID);
    try {
      // 1. Ensure tenant schema and Flyway migrations are up to date
      tenantSchemaProvisioningService.migrateTenantSchema(DEMO_TENANT_ID);

      // 2. Execute transactional seed script with dynamic dates relative to current time
      Resource seedScript = resourceLoader.getResource(SEED_SCRIPT_PATH);
      if (!seedScript.exists()) {
        throw new IllegalStateException("Seed script not found at path: " + SEED_SCRIPT_PATH);
      }

      executeSeedScript(seedScript);

      log.info(
          "Demo showcase workspace [{}] reset successfully completed with pristine records and fresh dates.",
          DEMO_TENANT_ID);
    } catch (Exception e) {
      log.error("Failed to reset demo showcase data: {}", e.getMessage(), e);
      throw new IllegalStateException("Failed to reset demo showcase data: " + e.getMessage(), e);
    }
  }

  private void executeSeedScript(Resource seedScript) throws SQLException {
    try (Connection connection = dataSource.getConnection()) {
      boolean originalAutoCommit = connection.getAutoCommit();
      try {
        connection.setAutoCommit(false);
        ScriptUtils.executeSqlScript(connection, seedScript);
        connection.commit();
      } catch (Exception ex) {
        connection.rollback();
        throw ex;
      } finally {
        connection.setAutoCommit(originalAutoCommit);
      }
    }
  }
}
