package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class SplunkEntityToDTOTest extends CategoryTest {
  @InjectMocks SplunkEntityToDTO splunkEntityToDTO;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(secretRefService.createSecretRef(any())).thenCallRealMethod();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateSplunkConnectorDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String splunkUrl = "https://splunk.dev.harness.io:8089";
    String accountId = "accountId";

    SplunkConnector splunkConnector = SplunkConnector.builder()
                                          .username(username)
                                          .passwordRef(encryptedPassword)
                                          .splunkUrl(splunkUrl)
                                          .accountId(accountId)
                                          .build();

    SplunkConnectorDTO splunkConnectorDTO = splunkEntityToDTO.createConnectorDTO(splunkConnector);
    assertThat(splunkConnectorDTO).isNotNull();
    assertThat(splunkConnectorDTO.getUsername()).isEqualTo(splunkConnector.getUsername());
    assertThat(splunkConnectorDTO.getPasswordRef().getIdentifier()).isEqualTo(splunkConnector.getPasswordRef());
    assertThat(splunkConnectorDTO.getSplunkUrl()).isEqualTo("https://splunk.dev.harness.io:8089/");
    assertThat(splunkConnectorDTO.getAccountId()).isEqualTo(splunkConnector.getAccountId());
  }
}
