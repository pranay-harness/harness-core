package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.appdynamicsconnector.AppDynamicsConnectorSummaryDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsConnectorSummaryMapper;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AppDynamicsConnectorSummaryMapperTest extends CategoryTest {
  @InjectMocks AppDynamicsConnectorSummaryMapper appDynamicsConnectorSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnectorSummaryDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnector appDynamicsConnector = AppDynamicsConnector.builder()
                                                    .username(username)
                                                    .passwordReference(encryptedPassword)
                                                    .accountname(accountname)
                                                    .controllerUrl(controllerUrl)
                                                    .accountId(accountId)
                                                    .build();

    AppDynamicsConnectorSummaryDTO appDynamicsConnectorSummaryDTO =
        appDynamicsConnectorSummaryMapper.toConnectorConfigSummaryDTO(appDynamicsConnector);

    assertThat(appDynamicsConnectorSummaryDTO).isNotNull();
    assertThat(appDynamicsConnectorSummaryDTO.getUsername()).isEqualTo(appDynamicsConnector.getUsername());
    assertThat(appDynamicsConnectorSummaryDTO.getAccountname()).isEqualTo(appDynamicsConnector.getAccountname());
    assertThat(appDynamicsConnectorSummaryDTO.getControllerUrl()).isEqualTo(appDynamicsConnector.getControllerUrl());
  }
}
