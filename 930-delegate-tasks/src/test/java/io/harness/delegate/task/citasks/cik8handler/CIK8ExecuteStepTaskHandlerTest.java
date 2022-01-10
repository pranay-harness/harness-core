/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.rule.OwnerRule.SHUBHAM;

import static junit.framework.TestCase.assertEquals;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ci.k8s.CIK8ExecuteStepTaskParams;
import io.harness.delegate.beans.ci.k8s.K8sTaskExecutionResponse;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

public class CIK8ExecuteStepTaskHandlerTest extends CategoryTest {
  @InjectMocks private CIK8ExecuteStepTaskHandler cik8ExecuteStepTaskHandler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void executeTaskInternalInvalidStep() {
    CIK8ExecuteStepTaskParams params = CIK8ExecuteStepTaskParams.builder().serializedStep("foo".getBytes()).build();
    K8sTaskExecutionResponse response = cik8ExecuteStepTaskHandler.executeTaskInternal(params, "");
    assertEquals(CommandExecutionStatus.FAILURE, response.getCommandExecutionStatus());
  }
}
