/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;
import static io.harness.k8s.kubectl.Utils.parseLatestRevisionNumberFromRolloutHistory;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.PUNEET;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class UtilsTest extends CategoryTest {
  @Test
  @Owner(developers = PUNEET)
  @Category(UnitTests.class)
  public void latestRevisionTest() {
    String rolloutHistory = "deployments \"demo1-nginx-deployment\"\n"
        + "REVISION  CHANGE-CAUSE\n"
        + "2         kubectl.exe apply --kubeconfig=.kubeconfig --filename=manifests.yaml --record=true --output=yaml\n"
        + "3         kubectl edit deploy/demo1-nginx-deployment\n"
        + "4         kubectl edit deploy/demo1-nginx-deployment\n"
        + "\n";

    assertThat(parseLatestRevisionNumberFromRolloutHistory(rolloutHistory)).isEqualTo("4");

    rolloutHistory = "daemonsets \"datadog-agent\"\n"
        + "REVISION  CHANGE-CAUSE\n"
        + "2         <none>\n"
        + "3         <none>\n"
        + "\n";

    assertThat(parseLatestRevisionNumberFromRolloutHistory(rolloutHistory)).isEqualTo("3");
  }

  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void encloseWithQuotesIfNeededTest() {
    assertThat(encloseWithQuotesIfNeeded("kubectl")).isEqualTo("kubectl");
    assertThat(encloseWithQuotesIfNeeded("kubectl ")).isEqualTo("kubectl");
    assertThat(encloseWithQuotesIfNeeded("config")).isEqualTo("config");
    assertThat(encloseWithQuotesIfNeeded("C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe"))
        .isEqualTo("\"C:\\Program Files\\Docker\\Docker\\Resources\\bin\\kubectl.exe\"");
  }
}
