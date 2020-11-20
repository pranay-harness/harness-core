package io.harness.connector.mappers.jira;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.connector.mappers.SecretRefHelper;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.Scope;
import io.harness.encryption.SecretRefData;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class JiraDTOToEntityTest extends CategoryTest {
  @InjectMocks JiraDTOToEntity jiraDTOToEntity;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
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
    JiraConnector jiraConnector = jiraDTOToEntity.toConnectorEntity(dto);
    assertThat(jiraConnector).isNotNull();
    assertThat(jiraConnector.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnector.getUsername()).isEqualTo(userName);
    assertThat(jiraConnector.getPasswordRef()).isEqualTo(SecretRefHelper.getSecretConfigString(passwordSecretRef));
  }
}
