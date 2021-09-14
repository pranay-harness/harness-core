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
import io.harness.capability.service.CapabilityService;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskGroup;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.service.intfc.AssignDelegateService;
import software.wings.service.intfc.DelegateService;
import software.wings.service.intfc.DelegateTaskServiceClassic;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class BlockingCapabilityPermissionsRecordHandlerTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock private DelegateTaskServiceClassic delegateTaskServiceClassic;
  @Mock private AssignDelegateService assignDelegateService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private CapabilityService capabilityService;
  @InjectMocks @Inject BlockingCapabilityPermissionsRecordHandler recordHandler;

  @Inject HPersistence persistence;
  private CapabilityTaskSelectionDetails taskSelectionDetails = CapabilityTaskSelectionDetails.builder()
                                                                    .taskGroup(TaskGroup.HTTP)
                                                                    .accountId(generateUuid())
                                                                    .blocked(true)
                                                                    .capabilityId(generateUuid())
                                                                    .build();

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithFFDisabled() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, taskSelectionDetails.getAccountId())).thenReturn(false);
    recordHandler.handle(taskSelectionDetails);
    verify(delegateTaskServiceClassic, never()).executeBatchCapabilityCheckTask(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithNoSubjectPermissions() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, taskSelectionDetails.getAccountId())).thenReturn(true);
    recordHandler.handle(taskSelectionDetails);
    verify(delegateTaskServiceClassic, never()).executeBatchCapabilityCheckTask(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithSubjectPermissionWithInactiveDelegate() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, taskSelectionDetails.getAccountId())).thenReturn(true);

    CapabilitySubjectPermission capabilitySubjectPermission = buildCapabilitySubjectPermission();
    persistence.save(capabilitySubjectPermission);
    when(assignDelegateService.retrieveActiveDelegates(taskSelectionDetails.getAccountId(), null))
        .thenReturn(Collections.singletonList("delegateId"));

    recordHandler.handle(taskSelectionDetails);

    verify(delegateTaskServiceClassic, never()).executeBatchCapabilityCheckTask(any(), any(), any(), any());
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandle() {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, taskSelectionDetails.getAccountId())).thenReturn(true);

    persistence.save(taskSelectionDetails);

    CapabilitySubjectPermission capabilitySubjectPermission = buildCapabilitySubjectPermission();
    persistence.save(capabilitySubjectPermission);
    when(assignDelegateService.retrieveActiveDelegates(taskSelectionDetails.getAccountId(), null))
        .thenReturn(Collections.singletonList(capabilitySubjectPermission.getDelegateId()));

    recordHandler.handle(taskSelectionDetails);

    ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
    verify(delegateTaskServiceClassic)
        .executeBatchCapabilityCheckTask(eq(capabilitySubjectPermission.getAccountId()),
            eq(capabilitySubjectPermission.getDelegateId()), argumentCaptor.capture(),
            eq(taskSelectionDetails.getUuid()));

    List<CapabilitySubjectPermission> capabilitySubjectPermissions = argumentCaptor.getValue();
    assertThat(capabilitySubjectPermissions).hasSize(1);
    assertThat(capabilitySubjectPermissions.get(0)).isEqualTo(capabilitySubjectPermission);

    CapabilitySubjectPermission updatedCapabilitySubjectPermission =
        persistence.get(CapabilitySubjectPermission.class, capabilitySubjectPermission.getUuid());
    assertThat(updatedCapabilitySubjectPermission.getRevalidateAfter()).isGreaterThan(System.currentTimeMillis());
  }

  private CapabilitySubjectPermission buildCapabilitySubjectPermission() {
    return CapabilitySubjectPermission.builder()
        .permissionResult(PermissionResult.UNCHECKED)
        .revalidateAfter(System.currentTimeMillis() - 5000)
        .maxValidUntil(System.currentTimeMillis() + 60000)
        .delegateId(generateUuid())
        .accountId(taskSelectionDetails.getAccountId())
        .capabilityId(taskSelectionDetails.getCapabilityId())
        .build();
  }
}
