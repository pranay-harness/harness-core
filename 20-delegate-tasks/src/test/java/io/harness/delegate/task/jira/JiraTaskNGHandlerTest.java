package io.harness.delegate.task.jira;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;
import net.rcarz.jiraclient.Resource;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;

public class JiraTaskNGHandlerTest extends CategoryTest {
  @Rule public WireMockRule wireMockRule = new WireMockRule(6565);

  private final JiraTaskNGHandler jiraTaskNGHandler = new JiraTaskNGHandler();

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentials() {
    final String validUrl = "http://localhost:6565";
    wireMockRule.stubFor(
        WireMock.get(WireMock.urlEqualTo(Resource.getBaseUri() + "project"))
            .willReturn(WireMock.aResponse().withBody("[]").withHeader("Content-type", "application/json")));
    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.validateCredentials(createJiraTaskParameters(validUrl));
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentialsError() {
    final String wrongUrl = "localhost:6565";
    wireMockRule.stubFor(
        WireMock.get(WireMock.urlEqualTo(Resource.getBaseUri() + "project"))
            .willReturn(WireMock.aResponse().withBody("[]").withHeader("Content-type", "application/json")));
    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.validateCredentials(createJiraTaskParameters(wrongUrl));
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  private JiraTaskNGParameters createJiraTaskParameters(String url) {
    JiraConnectorDTO jiraConnectorDTO =
        JiraConnectorDTO.builder()
            .jiraUrl(url)
            .username("username")
            .passwordRef(SecretRefData.builder().decryptedValue(new char[] {'3', '4', 'f', '5', '1'}).build())
            .build();

    return JiraTaskNGParameters.builder().jiraConnectorDTO(jiraConnectorDTO).build();
  }
}
