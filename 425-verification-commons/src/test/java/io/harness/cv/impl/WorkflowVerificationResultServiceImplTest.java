/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cv.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.persistence.HQuery.excludeAuthority;
import static io.harness.rule.OwnerRule.RAGHU;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.cv.VerificationCommonsTestBase;
import io.harness.cv.WorkflowVerificationResult;
import io.harness.cv.api.WorkflowVerificationResultService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WorkflowVerificationResultServiceImplTest extends VerificationCommonsTestBase {
  @Inject private HPersistence hPersistence;
  @Inject private WorkflowVerificationResultService workflowVerificationResultService;

  private String accountId;
  private String appId;
  private String serviceId;
  private String envId;
  private String workflowId;
  private String stateExecutionId;

  @Before
  public void setUp() {
    accountId = generateUuid();
    appId = generateUuid();
    serviceId = generateUuid();
    envId = generateUuid();
    workflowId = generateUuid();
    stateExecutionId = generateUuid();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testAddWorkflowVerificationResult() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(stateExecutionId)
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType("PROMETHEUS")
                                                                        .executionStatus(ExecutionStatus.SKIPPED)
                                                                        .build());
    List<WorkflowVerificationResult> workflowVerificationResults =
        hPersistence.createQuery(WorkflowVerificationResult.class, excludeAuthority).asList();
    assertThat(workflowVerificationResults.size()).isEqualTo(1);
    WorkflowVerificationResult workflowVerificationResult = workflowVerificationResults.get(0);
    assertThat(workflowVerificationResult.getAccountId()).isEqualTo(accountId);
    assertThat(workflowVerificationResult.getAppId()).isEqualTo(appId);
    assertThat(workflowVerificationResult.getStateExecutionId()).isEqualTo(stateExecutionId);
    assertThat(workflowVerificationResult.getServiceId()).isEqualTo(serviceId);
    assertThat(workflowVerificationResult.getEnvId()).isEqualTo(envId);
    assertThat(workflowVerificationResult.getWorkflowId()).isEqualTo(workflowId);
    assertThat(workflowVerificationResult.getStateType()).isEqualTo("PROMETHEUS");
    assertThat(workflowVerificationResult.isAnalyzed()).isFalse();
    assertThat(workflowVerificationResult.isRollback()).isFalse();
  }

  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void testUpdateWorkflowVerificationResult() {
    workflowVerificationResultService.addWorkflowVerificationResult(WorkflowVerificationResult.builder()
                                                                        .accountId(accountId)
                                                                        .appId(appId)
                                                                        .stateExecutionId(stateExecutionId)
                                                                        .serviceId(serviceId)
                                                                        .envId(envId)
                                                                        .workflowId(workflowId)
                                                                        .stateType("PROMETHEUS")
                                                                        .executionStatus(ExecutionStatus.RUNNING)
                                                                        .build());
    WorkflowVerificationResult workflowVerificationResult =
        hPersistence.createQuery(WorkflowVerificationResult.class, excludeAuthority).get();
    assertThat(workflowVerificationResult.getMessage()).isNull();
    assertThat(workflowVerificationResult.getExecutionStatus()).isEqualTo(ExecutionStatus.RUNNING);
    assertThat(workflowVerificationResult.isAnalyzed()).isFalse();
    assertThat(workflowVerificationResult.isRollback()).isFalse();

    String message = generateUuid();
    workflowVerificationResultService.updateWorkflowVerificationResult(
        stateExecutionId, true, ExecutionStatus.FAILED, message);
    workflowVerificationResult = hPersistence.createQuery(WorkflowVerificationResult.class, excludeAuthority).get();
    assertThat(workflowVerificationResult.getMessage()).isEqualTo(message);
    assertThat(workflowVerificationResult.getExecutionStatus()).isEqualTo(ExecutionStatus.FAILED);
    assertThat(workflowVerificationResult.isAnalyzed()).isTrue();
    assertThat(workflowVerificationResult.isRollback()).isTrue();
  }
}
