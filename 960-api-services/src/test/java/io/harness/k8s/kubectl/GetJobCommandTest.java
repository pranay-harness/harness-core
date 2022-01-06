/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.k8s.kubectl;

import static io.harness.rule.OwnerRule.VAIBHAV_SI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class GetJobCommandTest extends CategoryTest {
  @Test
  @Owner(developers = VAIBHAV_SI)
  @Category(UnitTests.class)
  public void command() {
    Kubectl client = Kubectl.client("kubectl", "CONFIG_PATH");
    GetJobCommand getJobCommand = new GetJobCommand(new GetCommand(client), "JOB1", "NAMESPACE");
    getJobCommand.output("jsonpath=.status");

    String command = getJobCommand.command();

    assertThat(command).isEqualTo(
        "kubectl --kubeconfig=CONFIG_PATH get jobs JOB1 --namespace=NAMESPACE --output=jsonpath=.status");
  }
}
