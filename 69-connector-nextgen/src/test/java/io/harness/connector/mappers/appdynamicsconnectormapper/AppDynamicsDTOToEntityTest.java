package io.harness.connector.mappers.appdynamicsconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.appdynamicsmapper.AppDynamicsDTOToEntity;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class AppDynamicsDTOToEntityTest extends CategoryTest {
  @InjectMocks AppDynamicsDTOToEntity appDynamicsDTOToEntity;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToAppDynamicsConnector() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String accountname = "accountname";
    String controllerUrl = "controllerUrl";
    String accountId = "accountId";

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .username(username)
                                                          .passwordReference(encryptedPassword)
                                                          .accountname(accountname)
                                                          .controllerUrl(controllerUrl)
                                                          .accountId(accountId)
                                                          .build();

    AppDynamicsConnector appDynamicsConnector = appDynamicsDTOToEntity.toConnectorEntity(appDynamicsConnectorDTO);
    assertThat(appDynamicsConnector).isNotNull();
    assertThat(appDynamicsConnector.getUsername()).isEqualTo(appDynamicsConnectorDTO.getUsername());
    assertThat(appDynamicsConnector.getPasswordReference()).isEqualTo(appDynamicsConnectorDTO.getPasswordReference());
    assertThat(appDynamicsConnector.getAccountname()).isEqualTo(appDynamicsConnectorDTO.getAccountname());
    assertThat(appDynamicsConnector.getControllerUrl()).isEqualTo(appDynamicsConnectorDTO.getControllerUrl());
    assertThat(appDynamicsConnector.getAccountId()).isEqualTo(appDynamicsConnectorDTO.getAccountId());
  }
}
