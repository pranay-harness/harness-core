/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.task.executioncapability;

import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_SYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.HttpConnectionParameters;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.rule.Owner;
import io.harness.utils.Functions;

import software.wings.WingsBaseTest;
import software.wings.beans.TaskType;

import com.google.common.collect.ImmutableList;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class BatchCapabilityCheckTaskTest extends WingsBaseTest {
  @Mock private ProtoCapabilityCheck capabilityCheck;
  @Mock private ProtoCapabilityCheckFactory capabilityCheckFactory;

  @InjectMocks
  private final BatchCapabilityCheckTask task =
      spy(new BatchCapabilityCheckTask(DelegateTaskPackage.builder()
                                           .delegateId("delegateId")
                                           .data(TaskData.builder()
                                                     .async(false)
                                                     .taskType(TaskType.BATCH_CAPABILITY_CHECK.name())
                                                     .timeout(DEFAULT_SYNC_CALL_TIMEOUT)
                                                     .build())
                                           .build(),
          null, Functions::doNothing, Functions::staticTruth));

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldThrowNotImplementedException() {
    assertThatThrownBy(() -> task.run(new Object[0])).isInstanceOf(NotImplementedException.class);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRunTaskAndReturnUncheckedResult() {
    List<CapabilityCheckDetails> capabilityCheckDetailsList =
        ImmutableList.<CapabilityCheckDetails>builder().add(CapabilityCheckDetails.builder().build()).build();
    BatchCapabilityCheckTaskParameters taskParams =
        BatchCapabilityCheckTaskParameters.builder().capabilityCheckDetailsList(capabilityCheckDetailsList).build();

    BatchCapabilityCheckTaskResponse taskResponse = (BatchCapabilityCheckTaskResponse) task.run(taskParams);

    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse.getCapabilityCheckDetailsList()).isNotNull();
    assertThat(taskResponse.getCapabilityCheckDetailsList()).hasSize(1);
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getPermissionResult())
        .isEqualTo(PermissionResult.UNCHECKED);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void shouldRunTaskAndReturnValidatedResult() {
    CapabilityCheckDetails capabilityCheckDetails =
        CapabilityCheckDetails.builder()
            .accountId(generateUuid())
            .capabilityId(generateUuid())
            .delegateId(generateUuid())
            .capabilityType(CapabilityType.ALWAYS_TRUE)
            .capabilityParameters(CapabilityParameters.newBuilder()
                                      .setHttpConnectionParameters(
                                          HttpConnectionParameters.newBuilder().setUrl("https://google.com").build())
                                      .build())
            .build();

    List<CapabilityCheckDetails> capabilityCheckDetailsList =
        ImmutableList.<CapabilityCheckDetails>builder().add(capabilityCheckDetails).build();

    BatchCapabilityCheckTaskParameters taskParams =
        BatchCapabilityCheckTaskParameters.builder().capabilityCheckDetailsList(capabilityCheckDetailsList).build();

    when(capabilityCheckFactory.obtainCapabilityCheck(capabilityCheckDetails.getCapabilityParameters()))
        .thenReturn(capabilityCheck);

    when(capabilityCheck.performCapabilityCheckWithProto(capabilityCheckDetails.getCapabilityParameters()))
        .thenReturn(CapabilitySubjectPermission.builder().permissionResult(PermissionResult.ALLOWED).build());

    BatchCapabilityCheckTaskResponse taskResponse = (BatchCapabilityCheckTaskResponse) task.run(taskParams);

    assertThat(taskResponse).isNotNull();
    assertThat(taskResponse.getCapabilityCheckDetailsList()).isNotNull();
    assertThat(taskResponse.getCapabilityCheckDetailsList()).hasSize(1);
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getAccountId())
        .isEqualTo(capabilityCheckDetails.getAccountId());
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getCapabilityId())
        .isEqualTo(capabilityCheckDetails.getCapabilityId());
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getDelegateId())
        .isEqualTo(capabilityCheckDetails.getDelegateId());
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getCapabilityType())
        .isEqualTo(capabilityCheckDetails.getCapabilityType());
    assertThat(taskResponse.getCapabilityCheckDetailsList().get(0).getPermissionResult())
        .isEqualTo(PermissionResult.ALLOWED);
  }
}
