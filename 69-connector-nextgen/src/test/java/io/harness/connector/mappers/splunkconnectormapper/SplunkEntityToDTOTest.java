package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectorDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class SplunkEntityToDTOTest extends CategoryTest {
  @InjectMocks SplunkEntityToDTO splunkEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateSplunkConnectorDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String splunkUrl = "splunkUrl";
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
    assertThat(splunkConnectorDTO.getSplunkUrl()).isEqualTo(splunkConnector.getSplunkUrl());
    assertThat(splunkConnectorDTO.getAccountId()).isEqualTo(splunkConnector.getAccountId());
  }
}
