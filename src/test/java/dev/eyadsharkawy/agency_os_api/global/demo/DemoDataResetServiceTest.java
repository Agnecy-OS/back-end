package dev.eyadsharkawy.agency_os_api.global.demo;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import dev.eyadsharkawy.agency_os_api.global.workspace.service.TenantSchemaProvisioningService;
import java.io.ByteArrayInputStream;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

@ExtendWith(MockitoExtension.class)
class DemoDataResetServiceTest {

  @Mock private DataSource dataSource;
  @Mock private TenantSchemaProvisioningService tenantSchemaProvisioningService;
  @Mock private ResourceLoader resourceLoader;
  @Mock private Connection connection;
  @Mock private Resource resource;

  @InjectMocks private DemoDataResetService demoDataResetService;

  @Test
  @DisplayName("resetDemoData should migrate tenant schema and execute seed script successfully")
  void resetDemoData_Success() throws Exception {
    when(resourceLoader.getResource("classpath:db/seed/demo-showcase-data.sql"))
        .thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream()).thenReturn(new ByteArrayInputStream("SELECT 1;".getBytes()));
    when(dataSource.getConnection()).thenReturn(connection);
    java.sql.Statement statement = mock(java.sql.Statement.class);
    when(connection.createStatement()).thenReturn(statement);
    when(statement.getUpdateCount()).thenReturn(-1);
    when(connection.getAutoCommit()).thenReturn(true);

    demoDataResetService.resetDemoData();

    verify(tenantSchemaProvisioningService)
        .migrateTenantSchema(DemoDataResetService.DEMO_TENANT_ID);
    verify(connection).setAutoCommit(false);
    verify(connection).commit();
    verify(connection).setAutoCommit(true);
  }

  @Test
  @DisplayName("resetDemoData should throw when seed script does not exist")
  void resetDemoData_ScriptNotFound_Throws() {
    when(resourceLoader.getResource("classpath:db/seed/demo-showcase-data.sql"))
        .thenReturn(resource);
    when(resource.exists()).thenReturn(false);

    assertThatThrownBy(() -> demoDataResetService.resetDemoData())
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Seed script not found");
  }

  @Test
  @DisplayName("resetDemoData should rollback connection when script execution fails")
  void resetDemoData_ScriptFails_RollsBack() throws Exception {
    when(resourceLoader.getResource("classpath:db/seed/demo-showcase-data.sql"))
        .thenReturn(resource);
    when(resource.exists()).thenReturn(true);
    when(resource.getInputStream())
        .thenReturn(new ByteArrayInputStream("INVALID SQL STATEMENT;".getBytes()));
    when(dataSource.getConnection()).thenReturn(connection);
    when(connection.getAutoCommit()).thenReturn(true);
    when(connection.createStatement()).thenThrow(new SQLException("Syntax error"));

    assertThatThrownBy(() -> demoDataResetService.resetDemoData())
        .isInstanceOf(IllegalStateException.class);

    verify(connection).rollback();
  }
}
