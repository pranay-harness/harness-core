package io.harness.connector;

import static io.harness.rule.OwnerRule.DEEPAK;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FullyQualitifedIdentifierHelperTest extends CategoryTest {
  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void getFullyQualifiedIdentifier() {
    String accountIdentifier = "accountIdentifier";
    String orgIdentifier = "orgIdentifier";
    String projectIdentifier = "projectIdentifier";
    String connectorIdentifier = "connectorIdentifier";

    // FQN for a account level identifier
    String accountLevelFQN =
        FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier, null, null, connectorIdentifier);
    assertThat(accountLevelFQN).isEqualTo("accountIdentifier/connectorIdentifier");
    String orgLevelFQN = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, null, connectorIdentifier);
    assertThat(orgLevelFQN).isEqualTo("accountIdentifier/orgIdentifier/connectorIdentifier");
    String projectLevelFQN = FullyQualitifedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    assertThat(projectLevelFQN).isEqualTo("accountIdentifier/orgIdentifier/projectIdentifier/connectorIdentifier");
  }
}