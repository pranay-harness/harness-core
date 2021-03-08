package io.harness.connector.mappers.jira;

import static io.harness.encryption.Scope.ACCOUNT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.connector.entities.embedded.jira.JiraConnector;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.encryption.SecretRefHelper;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class JiraEntityToDTOTest extends CategoryTest {
  @InjectMocks JiraEntityToDTO jiraEntityToDTO;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = OwnerRule.PRASHANT)
  @Category(UnitTests.class)
  public void createConnectorDTOTest() {
    String jiraUrl = "url";
    String userName = "dockerUserName";
    String passwordRef = ACCOUNT + ".passwordRef";

    JiraConnector jiraConnector =
        JiraConnector.builder().jiraUrl(jiraUrl).username(userName).passwordRef(passwordRef).build();

    JiraConnectorDTO jiraConnectorDTO = jiraEntityToDTO.createConnectorDTO(jiraConnector);
    assertThat(jiraConnectorDTO).isNotNull();
    assertThat(jiraConnectorDTO.getJiraUrl()).isEqualTo(jiraUrl);
    assertThat(jiraConnectorDTO.getUsername()).isEqualTo(userName);
    assertThat(jiraConnectorDTO.getPasswordRef()).isEqualTo(SecretRefHelper.createSecretRef(passwordRef));
  }
}
