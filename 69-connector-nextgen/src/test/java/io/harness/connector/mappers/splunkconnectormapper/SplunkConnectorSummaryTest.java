package io.harness.connector.mappers.splunkconnectormapper;

import static io.harness.rule.OwnerRule.NEMANJA;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.apis.dto.splunkconnector.SplunkConnectorSummaryDTO;
import io.harness.connector.entities.embedded.splunkconnector.SplunkConnector;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class SplunkConnectorSummaryTest extends CategoryTest {
  @InjectMocks SplunkConnectorSummaryMapper splunkConnectorSummaryMapper;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testCreateSplunkConnectorSummaryDTO() {
    String username = "username";
    String encryptedPassword = "encryptedPassword";
    String splunkUrl = "splunkUrl";
    String accountId = "accountId";

    SplunkConnector splunkConnector = SplunkConnector.builder()
                                          .username(username)
                                          .passwordReference(encryptedPassword)
                                          .splunkUrl(splunkUrl)
                                          .accountId(accountId)
                                          .build();

    SplunkConnectorSummaryDTO splunkConnectorSummaryDTO =
        splunkConnectorSummaryMapper.toConnectorConfigSummaryDTO(splunkConnector);

    assertThat(splunkConnectorSummaryDTO).isNotNull();
    assertThat(splunkConnectorSummaryDTO.getUsername()).isEqualTo(splunkConnector.getUsername());
    assertThat(splunkConnectorSummaryDTO.getSplunkUrl()).isEqualTo(splunkConnector.getSplunkUrl());
  }
}
