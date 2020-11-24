package io.harness.connector.impl;

import static io.harness.delegate.beans.connector.ConnectorType.APP_DYNAMICS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.ConnectorDTO;
import io.harness.connector.apis.dto.ConnectorInfoDTO;
import io.harness.connector.apis.dto.ConnectorResponseDTO;
import io.harness.connector.entities.embedded.appdynamicsconnector.AppDynamicsConnector;
import io.harness.connector.mappers.ConnectorMapper;
import io.harness.connector.validator.ConnectionValidator;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.repositories.ConnectorRepository;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import java.util.Map;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.junit.runners.MethodSorters;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class AppDynamicsConnectorTest extends CategoryTest {
  @Mock ConnectorMapper connectorMapper;
  @Mock ConnectorRepository connectorRepository;
  @Mock private Map<String, ConnectionValidator> connectionValidatorMap;
  @InjectMocks DefaultConnectorServiceImpl connectorService;

  String userName = "userName";
  String password = "password";
  String identifier = "identifier";
  String name = "name";
  String controllerUrl = "https://xwz.com/";
  String accountName = "accountName";
  ConnectorDTO connectorRequest;
  ConnectorResponseDTO connectorResponse;
  AppDynamicsConnector appDynamicsConfig;
  String accountIdentifier = "accountIdentifier";
  String secretIdentifier = "secretIdentifier";
  @Rule public ExpectedException expectedEx = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    appDynamicsConfig = AppDynamicsConnector.builder()
                            .username(userName)
                            .accountId(accountIdentifier)
                            .accountname(accountName)
                            .controllerUrl(controllerUrl)
                            .passwordRef(password)
                            .build();
    appDynamicsConfig.setType(APP_DYNAMICS);
    appDynamicsConfig.setIdentifier(identifier);
    appDynamicsConfig.setName(name);

    SecretRefData secretRefData = SecretRefData.builder().identifier(secretIdentifier).scope(Scope.ACCOUNT).build();

    AppDynamicsConnectorDTO appDynamicsConnectorDTO = AppDynamicsConnectorDTO.builder()
                                                          .username(userName)
                                                          .accountId(accountIdentifier)
                                                          .accountname(accountName)
                                                          .controllerUrl(controllerUrl)
                                                          .passwordRef(secretRefData)
                                                          .build();

    ConnectorInfoDTO connectorInfo = ConnectorInfoDTO.builder()
                                         .name(name)
                                         .identifier(identifier)
                                         .connectorType(APP_DYNAMICS)
                                         .connectorConfig(appDynamicsConnectorDTO)
                                         .build();
    connectorResponse = ConnectorResponseDTO.builder().connector(connectorInfo).build();
    when(connectorRepository.save(appDynamicsConfig)).thenReturn(appDynamicsConfig);
    when(connectorMapper.writeDTO(appDynamicsConfig)).thenReturn(connectorResponse);
    when(connectorMapper.toConnector(connectorRequest, accountIdentifier)).thenReturn(appDynamicsConfig);
  }

  private ConnectorResponseDTO createConnector() {
    return connectorService.create(connectorRequest, accountIdentifier);
  }

  @Test
  @Owner(developers = OwnerRule.NEMANJA)
  @Category(UnitTests.class)
  public void testCreateAppDynamicsConnector() {
    ConnectorResponseDTO connectorDTOOutput = createConnector();
    ensureAppDynamicsConnectorFieldsAreCorrect(connectorDTOOutput);
  }

  @Test
  @Owner(developers = OwnerRule.NEMANJA)
  @Category(UnitTests.class)
  public void testGetAppDynamicsConnector() {
    createConnector();
    when(connectorRepository.findByFullyQualifiedIdentifierAndDeletedNot(anyString(), anyBoolean()))
        .thenReturn(Optional.of(appDynamicsConfig));
    ConnectorResponseDTO connectorDTO = connectorService.get(accountIdentifier, null, null, identifier).get();
    ensureAppDynamicsConnectorFieldsAreCorrect(connectorDTO);
  }

  private void ensureAppDynamicsConnectorFieldsAreCorrect(ConnectorResponseDTO connectorResponse) {
    ConnectorInfoDTO connector = connectorResponse.getConnector();
    assertThat(connector).isNotNull();
    assertThat(connector.getName()).isEqualTo(name);
    assertThat(connector.getIdentifier()).isEqualTo(identifier);
    assertThat(connector.getConnectorType()).isEqualTo(APP_DYNAMICS);
    AppDynamicsConnectorDTO appDynamicsConnectorDTO = (AppDynamicsConnectorDTO) connector.getConnectorConfig();
    assertThat(appDynamicsConnectorDTO).isNotNull();
    assertThat(appDynamicsConnectorDTO.getUsername()).isEqualTo(userName);
    assertThat(appDynamicsConnectorDTO.getPasswordRef()).isNotNull();
    assertThat(appDynamicsConnectorDTO.getPasswordRef().getIdentifier()).isEqualTo(secretIdentifier);
    assertThat(appDynamicsConnectorDTO.getPasswordRef().getScope()).isEqualTo(Scope.ACCOUNT);
    assertThat(appDynamicsConnectorDTO.getAccountname()).isEqualTo(accountName);
    assertThat(appDynamicsConnectorDTO.getControllerUrl()).isEqualTo(controllerUrl);
    assertThat(appDynamicsConnectorDTO.getAccountId()).isEqualTo(accountIdentifier);
  }
}
