package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.appdynamicsconnector.AppDynamicsConfigSummaryDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConfig;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsConfigSummaryMapper;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AppDynamicsConfigSummaryMapperTest extends CategoryTest {
  @InjectMocks AppDynamicsConfigSummaryMapper appDynamicsConfigSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConfigSummaryDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConfig appDynamicsConfig = AppDynamicsConfig.builder()
                                              .username(username)
                                              .passwordReference(encryptedPassword)
                                              .accountname(accountname)
                                              .controllerUrl(controllerUrl)
                                              .accountId(accountId)
                                              .build();

    AppDynamicsConfigSummaryDTO appDynamicsConfigSummaryDTO =
        appDynamicsConfigSummaryMapper.toConnectorConfigSummaryDTO(appDynamicsConfig);

    assertThat(appDynamicsConfigSummaryDTO).isNotNull();
    assertThat(appDynamicsConfigSummaryDTO.getUsername()).isEqualTo(appDynamicsConfig.getUsername());
    assertThat(appDynamicsConfigSummaryDTO.getAccountname()).isEqualTo(appDynamicsConfig.getAccountname());
    assertThat(appDynamicsConfigSummaryDTO.getControllerUrl()).isEqualTo(appDynamicsConfig.getControllerUrl());
  }
}
