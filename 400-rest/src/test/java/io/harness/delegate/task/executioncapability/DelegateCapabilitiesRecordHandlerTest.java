/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.CapabilityTaskSelectionDetails;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.Delegate;
import io.harness.delegate.beans.TaskGroup;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateCapabilitiesRecordHandlerTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private DelegateTaskServiceClassic delegateTaskServiceClassic;
  @Mock FeatureFlagService featureFlagService;
  @InjectMocks @Inject DelegateCapabilitiesRecordHandler recordHandler;

  @Inject HPersistence persistence;
  private Delegate delegate = Delegate.builder().accountId(generateUuid()).uuid(generateUuid()).build();

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithFFDisabled() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(false);
    recordHandler.handle(delegate);
    verify(delegateTaskServiceClassic, never())
        .executeBatchCapabilityCheckTask(
            eq(delegate.getAccountId()), eq(delegate.getUuid()), any(List.class), eq(null));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithNoSubjectPermission() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(true);
    recordHandler.handle(delegate);
    verify(delegateTaskServiceClassic, never())
        .executeBatchCapabilityCheckTask(
            eq(delegate.getAccountId()), eq(delegate.getUuid()), any(List.class), eq(null));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandle() throws InterruptedException {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(true);

    // Blocked UNCHECKED capability
    CapabilityTaskSelectionDetails blockedTaskSelectionDetails = buildBlockedCapabilityTaskSelectionDetails();
    persistence.save(blockedTaskSelectionDetails);
    CapabilitySubjectPermission blockingCapabilitySubjectPermission = buildCapabilitySubjectPermission();
    blockingCapabilitySubjectPermission.setCapabilityId(blockedTaskSelectionDetails.getCapabilityId());
    persistence.save(blockingCapabilitySubjectPermission);

    // Non blocked UNCHECKED capability
    CapabilityTaskSelectionDetails nonBlockedTaskSelectionDetails = buildBlockedCapabilityTaskSelectionDetails();
    nonBlockedTaskSelectionDetails.setTaskGroup(TaskGroup.SHELL_SCRIPT_PROVISION);
    nonBlockedTaskSelectionDetails.setBlocked(false);
    persistence.save(nonBlockedTaskSelectionDetails);
    CapabilitySubjectPermission nonBlockingCapabilitySubjectPermission = buildCapabilitySubjectPermission();
    nonBlockingCapabilitySubjectPermission.setCapabilityId(nonBlockedTaskSelectionDetails.getCapabilityId());
    persistence.save(nonBlockingCapabilitySubjectPermission);

    // Capability with ALLOWED permission
    CapabilitySubjectPermission capabilitySubjectPermission = buildCapabilitySubjectPermission();
    capabilitySubjectPermission.setPermissionResult(PermissionResult.ALLOWED);
    persistence.save(capabilitySubjectPermission);

    recordHandler.handle(delegate);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(delegateTaskServiceClassic)
        .executeBatchCapabilityCheckTask(
            eq(delegate.getAccountId()), eq(delegate.getUuid()), argumentCaptor.capture(), eq(null));

    List<CapabilitySubjectPermission> capabilitySubjectPermissions = argumentCaptor.getValue();
    assertThat(capabilitySubjectPermissions).hasSize(2);
    assertThat(capabilitySubjectPermissions)
        .containsExactlyInAnyOrder(capabilitySubjectPermission, nonBlockingCapabilitySubjectPermission);
  }

  private CapabilitySubjectPermission buildCapabilitySubjectPermission() {
    return CapabilitySubjectPermission.builder()
        .permissionResult(PermissionResult.UNCHECKED)
        .revalidateAfter(System.currentTimeMillis() - 5000)
        .maxValidUntil(System.currentTimeMillis() + 60000)
        .delegateId(delegate.getUuid())
        .accountId(delegate.getAccountId())
        .capabilityId(generateUuid())
        .build();
  }

  private CapabilityTaskSelectionDetails buildBlockedCapabilityTaskSelectionDetails() {
    return CapabilityTaskSelectionDetails.builder()
        .taskGroup(TaskGroup.HTTP)
        .accountId(delegate.getAccountId())
        .blocked(true)
        .capabilityId(generateUuid())
        .build();
  }
}
