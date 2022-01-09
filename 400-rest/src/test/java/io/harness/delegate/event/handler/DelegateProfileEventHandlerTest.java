/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.event.handler;

import static io.harness.beans.FeatureName.TRIGGER_PROFILE_SCRIPT_EXECUTION_WF;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.event.handler.DelegateProfileEventHandler.MUST_EXECUTE_ON_DELEGATE_VAR_NAME;
import static io.harness.delegate.event.handler.DelegateProfileEventHandler.PROFILE_SCRIPT_CONTENT_VAR_NAME;
import static io.harness.delegate.event.handler.DelegateProfileEventHandler.PROFILE_SCRIPT_EXECUTION_WORKFLOW_NAME;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.WorkflowType;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.DelegateProfile;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.ExecutionArgs;
import software.wings.beans.Workflow;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.WorkflowExecutionService;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.List;
import java.util.Set;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateProfileEventHandlerTest extends WingsBaseTest {
  @Mock private WorkflowExecutionService workflowExecutionService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private DelegateService delegateService;
  @InjectMocks @Inject private DelegateProfileEventHandler delegateProfileEventHandler;
  @Inject private HPersistence persistence;

  private Workflow createWorkflow(String accountId) {
    Workflow workflow = new Workflow();
    workflow.setAccountId(accountId);
    workflow.setName(PROFILE_SCRIPT_EXECUTION_WORKFLOW_NAME);
    workflow.setAppId(generateUuid());
    persistence.save(workflow);

    return workflow;
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedShouldThrowNullPointerException() {
    assertThatThrownBy(() -> delegateProfileEventHandler.onProfileApplied(null, "delegateId", "profileId"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> delegateProfileEventHandler.onProfileApplied("accountId", null, "profileId"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> delegateProfileEventHandler.onProfileApplied("accountId", "delegateId", null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedShouldNotExecuteScriptLogic() {
    String accountId = generateUuid();
    createWorkflow(accountId);

    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(false);

    delegateProfileEventHandler.onProfileApplied("accountId", "delegateId", "profileId");

    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedShouldNotExecuteCapabilitiesLogic() {
    String accountId = generateUuid();

    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(false);

    delegateProfileEventHandler.onProfileApplied("accountId", "delegateId", "profileId");

    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
    verify(delegateService, never()).regenerateCapabilityPermissions(any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileSelectorsUpdatedShouldNotExecuteCapabilitiesLogic() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    delegateProfileEventHandler.onProfileSelectorsUpdated(accountId, profileId);

    verify(delegateService, never()).regenerateCapabilityPermissions(any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileScopesUpdatedShouldNotExecuteCapabilitiesLogic() {
    String accountId = generateUuid();
    String profileId = generateUuid();

    delegateProfileEventHandler.onProfileScopesUpdated(accountId, profileId);

    verify(delegateService, never()).regenerateCapabilityPermissions(any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedShouldFoundNoProfileOrScript() {
    String accountId = generateUuid();
    createWorkflow(accountId);
    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);

    // Test no profile found
    delegateProfileEventHandler.onProfileApplied("accountId", "delegateId", "profileId");
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());

    // Test script is empty
    DelegateProfile delegateProfile = DelegateProfile.builder().accountId(accountId).name("test").build();
    persistence.save(delegateProfile);

    delegateProfileEventHandler.onProfileApplied(accountId, "delegateId", delegateProfile.getUuid());
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedWithWorkflowNotPresent() {
    String accountId = generateUuid();
    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);

    DelegateProfile delegateProfile =
        DelegateProfile.builder().accountId(accountId).name("test").startupScript("echo test").build();
    persistence.save(delegateProfile);

    delegateProfileEventHandler.onProfileApplied(accountId, "delegateId", delegateProfile.getUuid());
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileAppliedShouldTriggerWorkflow() {
    String accountId = generateUuid();
    String delegateId = generateUuid();
    Workflow workflow = createWorkflow(accountId);
    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);

    DelegateProfile delegateProfile =
        DelegateProfile.builder().accountId(accountId).name("test").startupScript("echo test").build();
    persistence.save(delegateProfile);

    delegateProfileEventHandler.onProfileApplied(accountId, delegateId, delegateProfile.getUuid());

    ArgumentCaptor<ExecutionArgs> argumentCaptor = ArgumentCaptor.forClass(ExecutionArgs.class);
    verify(workflowExecutionService)
        .triggerOrchestrationExecution(
            eq(workflow.getAppId()), eq(null), eq(workflow.getUuid()), argumentCaptor.capture(), eq(null));

    ExecutionArgs executionArgs = argumentCaptor.getValue();
    assertThat(executionArgs.getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(executionArgs.getWorkflowVariables()).isNotEmpty();
    assertThat(executionArgs.getWorkflowVariables().get(MUST_EXECUTE_ON_DELEGATE_VAR_NAME)).isEqualTo(delegateId);
    assertThat(executionArgs.getWorkflowVariables().get(PROFILE_SCRIPT_CONTENT_VAR_NAME))
        .isEqualTo(delegateProfile.getStartupScript());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileUpdatedShouldThrowNullPointerException() {
    assertThatThrownBy(() -> delegateProfileEventHandler.onProfileUpdated(null, DelegateProfile.builder().build()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> delegateProfileEventHandler.onProfileUpdated(DelegateProfile.builder().build(), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileUpdatedShouldNotExecuteTheLogic() {
    String accountId = generateUuid();
    createWorkflow(accountId);

    // Test FF is not enabled
    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(false);
    delegateProfileEventHandler.onProfileUpdated(
        DelegateProfile.builder().accountId(accountId).build(), DelegateProfile.builder().accountId(accountId).build());
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());

    // Test FF enabled, but scripts are same or empty
    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);
    delegateProfileEventHandler.onProfileUpdated(
        DelegateProfile.builder().accountId(accountId).startupScript("echo test").build(),
        DelegateProfile.builder().accountId(accountId).startupScript("echo test").build());
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());

    delegateProfileEventHandler.onProfileUpdated(
        DelegateProfile.builder().accountId(accountId).startupScript("echo test").build(),
        DelegateProfile.builder().accountId(accountId).build());
    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileUpdatedShouldNotFoundAnyDelegatesForGivenProfile() {
    String accountId = generateUuid();
    createWorkflow(accountId);

    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);

    delegateProfileEventHandler.onProfileUpdated(DelegateProfile.builder().accountId(accountId).build(),
        DelegateProfile.builder().accountId(accountId).startupScript("echo test").build());

    verify(workflowExecutionService, never()).triggerOrchestrationExecution(any(), any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testOnProfileUpdatedShouldTriggerWorkflow() {
    String accountId = generateUuid();
    String updatedProfileId = generateUuid();
    Workflow workflow = createWorkflow(accountId);

    when(featureFlagService.isEnabled(TRIGGER_PROFILE_SCRIPT_EXECUTION_WF, accountId)).thenReturn(true);

    Delegate delegate1 =
        Delegate.builder().uuid(generateUuid()).accountId(accountId).delegateProfileId(updatedProfileId).build();
    Delegate delegate2 =
        Delegate.builder().uuid(generateUuid()).accountId(accountId).delegateProfileId(updatedProfileId).build();

    persistence.save(delegate1);
    persistence.save(delegate2);

    delegateProfileEventHandler.onProfileUpdated(
        DelegateProfile.builder().accountId(accountId).startupScript("echo test 1").build(),
        DelegateProfile.builder().accountId(accountId).startupScript("echo test 2").uuid(updatedProfileId).build());

    ArgumentCaptor<ExecutionArgs> argumentCaptor = ArgumentCaptor.forClass(ExecutionArgs.class);
    verify(workflowExecutionService, times(2))
        .triggerOrchestrationExecution(
            eq(workflow.getAppId()), eq(null), eq(workflow.getUuid()), argumentCaptor.capture(), eq(null));

    List<ExecutionArgs> executionArgs = argumentCaptor.getAllValues();
    assertThat(executionArgs.size()).isEqualTo(2);

    assertThat(executionArgs.get(0).getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(executionArgs.get(0).getWorkflowVariables()).isNotEmpty();
    assertThat(executionArgs.get(0).getWorkflowVariables().get(PROFILE_SCRIPT_CONTENT_VAR_NAME))
        .isEqualTo("echo test 2");

    assertThat(executionArgs.get(1).getWorkflowType()).isEqualTo(WorkflowType.ORCHESTRATION);
    assertThat(executionArgs.get(1).getWorkflowVariables()).isNotEmpty();
    assertThat(executionArgs.get(1).getWorkflowVariables().get(PROFILE_SCRIPT_CONTENT_VAR_NAME))
        .isEqualTo("echo test 2");

    Set<String> delegateIds =
        ImmutableSet.<String>builder()
            .add(executionArgs.get(0).getWorkflowVariables().get(MUST_EXECUTE_ON_DELEGATE_VAR_NAME))
            .add(executionArgs.get(1).getWorkflowVariables().get(MUST_EXECUTE_ON_DELEGATE_VAR_NAME))
            .build();

    assertThat(delegateIds).containsExactlyInAnyOrder(delegate1.getUuid(), delegate2.getUuid());

    verify(delegateService, never()).regenerateCapabilityPermissions(any(), any());
  }
}
