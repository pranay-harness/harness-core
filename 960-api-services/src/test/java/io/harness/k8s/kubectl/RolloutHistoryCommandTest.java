/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.PUNEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RolloutHistoryCommandTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void smokeTest() {
    Kubectl client = Kubectl.client(null, null);

    RolloutHistoryCommand rolloutHistoryCommand = client.rollout().history().resource("Deployment/nginx");

    assertThat(rolloutHistoryCommand.command()).isEqualTo("kubectl rollout history Deployment/nginx");
  }
}
