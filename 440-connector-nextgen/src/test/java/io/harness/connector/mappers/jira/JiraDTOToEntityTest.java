package io.harness.connector.mappers.jira;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.powermock.api.mockito.PowerMockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.ng.core.BaseNGAccess;
import io.harness.ng.service.SecretRefService;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class JiraDTOToEntityTest extends CategoryTest {
  @InjectMocks JiraDTOToEntity jiraDTOToEntity;
  @Mock SecretRefService secretRefService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(secretRefService.validateAndGetSecretConfigString(
             any(), BaseNGAccess.builder().accountIdentifier("accountIdentifier").build()))
        .thenCallRealMethod();
  }

  @Test
  @Owner(developers = OwnerRule.PRASHANT)
  @Category(UnitTests.class)
  public void toConnectorEntityTest() {
    String jiraUrl = "url";
    String userName = "userName";
    String passwordRefIdentifier = "passwordRefIdentifier";
    SecretRefData passwordSecretRef =
        SecretRefData.builder().identifier(passwordRefIdentifier).scope(Scope.ACCOUNT).build();

    JiraConnectorDTO dto =
        JiraConnectorDTO.builder().jiraUrl(jiraUrl).passwordRef(passwordSecretRef).username(userName).build();
    JiraConnector jiraConnector =
        jiraDTOToEntity.toConnectorEntity(dto, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build());
    assertThat(jiraConnector).isNotNull();
    assertThat(jiraConnector.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnector.getUsername()).isEqualTo(userName);
    assertThat(jiraConnector.getPasswordRef())
        .isEqualTo(secretRefService.validateAndGetSecretConfigString(
            passwordSecretRef, BaseNGAccess.builder().accountIdentifier("accountIdentifier").build()));
  }
}
