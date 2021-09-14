/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import static io.harness.rule.OwnerRule.DEEPAK;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class FullyQualifiedIdentifierHelperTest extends CategoryTest {
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
        FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(accountIdentifier, null, null, connectorIdentifier);
    assertThat(accountLevelFQN).isEqualTo("accountIdentifier/connectorIdentifier");
    String orgLevelFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, null, connectorIdentifier);
    assertThat(orgLevelFQN).isEqualTo("accountIdentifier/orgIdentifier/connectorIdentifier");
    String projectLevelFQN = FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(
        accountIdentifier, orgIdentifier, projectIdentifier, connectorIdentifier);
    assertThat(projectLevelFQN).isEqualTo("accountIdentifier/orgIdentifier/projectIdentifier/connectorIdentifier");
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testNoAccountIdentifierSentThrowsException() {
    String connectorIdentifier = "connectorIdentifier";
    assertThatThrownBy(
        () -> FullyQualifiedIdentifierHelper.getFullyQualifiedIdentifier(null, null, null, connectorIdentifier))
        .isInstanceOf(InvalidRequestException.class);
  }
}
