/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.rule.OwnerRule.ARVIND;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.REPOSITORY_NAME;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;

public class GitConnectionErrorAlertTest extends CategoryTest {
  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ARVIND)
  @Category(UnitTests.class)
  public void testMatch() {
    String connectorId = "CONNECTOR_ID";
    GitConnectionErrorAlert alert1 =
        GitConnectionErrorAlert.builder().accountId(ACCOUNT_ID).gitConnectorId(connectorId).branchName("b1").build();
    GitConnectionErrorAlert alert2 =
        GitConnectionErrorAlert.builder().accountId(ACCOUNT_ID).gitConnectorId(connectorId).branchName("b2").build();

    assertThat(alert1.matches(alert2)).isFalse();
    assertThat(alert2.matches(alert1)).isFalse();
    alert2.setBranchName("b1");
    assertThat(alert1.matches(alert2)).isTrue();
    assertThat(alert2.matches(alert1)).isTrue();
    alert1.setRepositoryName(REPOSITORY_NAME);
    assertThat(alert1.matches(alert2)).isFalse();
    assertThat(alert2.matches(alert1)).isFalse();
    alert2.setRepositoryName(REPOSITORY_NAME);
    assertThat(alert1.matches(alert2)).isTrue();
    assertThat(alert2.matches(alert1)).isTrue();
  }
}
