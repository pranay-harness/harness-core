package io.harness.cvng.core.services.api;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CvNextGenTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.cvng.beans.appd.AppDynamicsApplication;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.client.RequestExecutor;
import io.harness.cvng.client.VerificationManagerClient;
import io.harness.cvng.core.beans.AppdynamicsImportStatus;
import io.harness.cvng.core.entities.AppDynamicsCVConfig;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class AppDynamicsServiceTest extends CvNextGenTest {
  @Inject AppDynamicsService appDynamicsService;
  @Mock VerificationManagerClient verificationManagerClient;
  @Mock NextGenService nextGenService;
  @Mock private RequestExecutor requestExecutor;
  private String accountId;
  private String connectorIdentifier;

  @Before
  public void setup() throws IllegalAccessException {
    accountId = generateUuid();
    connectorIdentifier = generateUuid();
    FieldUtils.writeField(appDynamicsService, "verificationManagerClient", verificationManagerClient, true);
    FieldUtils.writeField(appDynamicsService, "nextGenService", nextGenService, true);
    FieldUtils.writeField(appDynamicsService, "requestExecutor", requestExecutor, true);
    when(nextGenService.get(anyString(), anyString(), anyString(), anyString())).then(invocation -> {
      Object[] args = invocation.getArguments();
      return Optional.of(ConnectorInfoDTO.builder().connectorConfig(AppDynamicsConnectorDTO.builder().build()).build());
    });
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void createMonitoringSource() {
    AppDynamicsCVConfig appDynamicsDSConfig1 = createAppDynamicsDataSourceCVConfig("App 1", "prod 1");
    AppDynamicsCVConfig appDynamicsDSConfig2 = createAppDynamicsDataSourceCVConfig("App 2", "prod 1");
    AppDynamicsCVConfig appDynamicsDSConfig3 = createAppDynamicsDataSourceCVConfig("App 3", "prod 2");
    List<CVConfig> cvConfigs = Arrays.asList(appDynamicsDSConfig1, appDynamicsDSConfig2, appDynamicsDSConfig3);
    List<AppDynamicsApplication> appDynamicsApplications = new ArrayList<>();
    for (int i = 0; i < 5; i++) {
      appDynamicsApplications.add(AppDynamicsApplication.builder().build());
    }
    when(requestExecutor.execute(any())).thenReturn(new RestResponse(appDynamicsApplications));
    AppdynamicsImportStatus appdynamicsImportStatus =
        (AppdynamicsImportStatus) appDynamicsService.createMonitoringSourceImportStatus(cvConfigs, 3);
    assertThat(appdynamicsImportStatus).isNotNull();
    assertThat(appdynamicsImportStatus.getNumberOfEnvironments()).isEqualTo(2);
    assertThat(appdynamicsImportStatus.getNumberOfApplications()).isEqualTo(3);
    assertThat(appdynamicsImportStatus.getTotalNumberOfApplications()).isEqualTo(5);
    assertThat(appdynamicsImportStatus.getTotalNumberOfEnvironments()).isEqualTo(3);
  }

  private AppDynamicsCVConfig createAppDynamicsDataSourceCVConfig(String applicationName, String envIdentifier) {
    AppDynamicsCVConfig appDynamicsDSConfig = new AppDynamicsCVConfig();
    appDynamicsDSConfig.setConnectorIdentifier(connectorIdentifier);
    appDynamicsDSConfig.setApplicationName(applicationName);
    appDynamicsDSConfig.setEnvIdentifier(envIdentifier);
    appDynamicsDSConfig.setAccountId(accountId);
    return appDynamicsDSConfig;
  }
}
