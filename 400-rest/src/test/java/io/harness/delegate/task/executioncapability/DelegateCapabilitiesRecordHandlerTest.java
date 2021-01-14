package io.harness.delegate.task.executioncapability;

import static io.harness.beans.FeatureName.PER_AGENT_CAPABILITIES;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.beans.DelegateTask;
import io.harness.capability.CapabilityParameters;
import io.harness.capability.CapabilityRequirement;
import io.harness.capability.CapabilitySubjectPermission;
import io.harness.capability.CapabilitySubjectPermission.PermissionResult;
import io.harness.capability.HttpConnectionParameters;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskRank;
import io.harness.delegate.beans.executioncapability.CapabilityType;
import io.harness.ff.FeatureFlagService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Delegate;
import software.wings.beans.TaskType;
import software.wings.service.intfc.DelegateService;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class DelegateCapabilitiesRecordHandlerTest extends WingsBaseTest {
  @Mock private DelegateService delegateService;
  @Mock FeatureFlagService featureFlagService;
  @InjectMocks @Inject DelegateCapabilitiesRecordHandler recordHandler;

  @Inject HPersistence persistence;
  private Delegate delegate = Delegate.builder().accountId(generateUuid()).uuid(generateUuid()).build();

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithFFDisabled() throws InterruptedException {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(false);
    recordHandler.handle(delegate);
    verify(delegateService, never()).executeTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithNoSubjectPermission() throws InterruptedException {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(true);
    recordHandler.handle(delegate);
    verify(delegateService, never()).executeTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandleWithNoCapabilityRequirement() throws InterruptedException {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(true);
    CapabilitySubjectPermission capabilitySubjectPermission = buildCapabilitySubjectPermission();
    persistence.save(capabilitySubjectPermission);

    recordHandler.handle(delegate);
    verify(delegateService, never()).executeTask(any(DelegateTask.class));
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void testHandle() throws InterruptedException {
    when(featureFlagService.isEnabled(PER_AGENT_CAPABILITIES, delegate.getAccountId())).thenReturn(true);
    CapabilitySubjectPermission capabilitySubjectPermission = buildCapabilitySubjectPermission();
    persistence.save(capabilitySubjectPermission);

    CapabilityRequirement capabilityRequirement =
        buildCapabilityRequirement(capabilitySubjectPermission.getCapabilityId());
    persistence.save(capabilityRequirement);

    BatchCapabilityCheckTaskResponse taskResponse =
        BatchCapabilityCheckTaskResponse.builder()
            .capabilityCheckDetailsList(Collections.singletonList(
                CapabilityCheckDetails.builder()
                    .accountId(delegate.getAccountId())
                    .delegateId(delegate.getUuid())
                    .capabilityId(capabilitySubjectPermission.getCapabilityId())
                    .capabilityType(CapabilityType.valueOf(capabilityRequirement.getCapabilityType()))
                    .capabilityParameters(capabilityRequirement.getCapabilityParameters())
                    .maxValidityPeriod(capabilityRequirement.getMaxValidityPeriod())
                    .revalidateAfterPeriod(capabilityRequirement.getRevalidateAfterPeriod())
                    .permissionResult(PermissionResult.ALLOWED)
                    .build()))
            .build();

    when(delegateService.executeTask(any(DelegateTask.class))).thenReturn(taskResponse);

    recordHandler.handle(delegate);

    ArgumentCaptor<DelegateTask> argumentCaptor = ArgumentCaptor.forClass(DelegateTask.class);
    verify(delegateService).executeTask(argumentCaptor.capture());

    DelegateTask delegateTask = argumentCaptor.getValue();
    assertThat(delegateTask).isNotNull();
    assertThat(delegateTask.getAccountId()).isEqualTo(delegate.getAccountId());
    assertThat(delegateTask.getMustExecuteOnDelegateId()).isEqualTo(delegate.getUuid());
    assertThat(delegateTask.getRank()).isEqualTo(DelegateTaskRank.CRITICAL);
    assertThat(delegateTask.getData()).isNotNull();
    assertThat(delegateTask.getData().getTaskType()).isEqualTo(TaskType.BATCH_CAPABILITY_CHECK.name());
    assertThat(delegateTask.getData().getParameters()).hasSize(1);
    assertThat(delegateTask.getData().getParameters()[0]).isInstanceOf(BatchCapabilityCheckTaskParameters.class);

    List<CapabilityCheckDetails> capabilityCheckDetailsList =
        ((BatchCapabilityCheckTaskParameters) delegateTask.getData().getParameters()[0])
            .getCapabilityCheckDetailsList();
    assertThat(capabilityCheckDetailsList).hasSize(1);
    assertThat(capabilityCheckDetailsList.get(0).getAccountId()).isEqualTo(delegate.getAccountId());
    assertThat(capabilityCheckDetailsList.get(0).getDelegateId()).isEqualTo(delegate.getUuid());
    assertThat(capabilityCheckDetailsList.get(0).getCapabilityId())
        .isEqualTo(capabilitySubjectPermission.getCapabilityId());
    assertThat(capabilityCheckDetailsList.get(0).getCapabilityType())
        .isEqualTo(CapabilityType.valueOf(capabilityRequirement.getCapabilityType()));
    assertThat(capabilityCheckDetailsList.get(0).getCapabilityParameters())
        .isEqualTo(capabilityRequirement.getCapabilityParameters());
    assertThat(capabilityCheckDetailsList.get(0).getPermissionResult()).isNull();
    assertThat(capabilityCheckDetailsList.get(0).getMaxValidityPeriod())
        .isEqualTo(capabilityRequirement.getMaxValidityPeriod());
    assertThat(capabilityCheckDetailsList.get(0).getRevalidateAfterPeriod())
        .isEqualTo(capabilityRequirement.getRevalidateAfterPeriod());

    CapabilitySubjectPermission updatedCapabilitySubjectPermission =
        persistence.get(CapabilitySubjectPermission.class, capabilitySubjectPermission.getUuid());

    assertThat(updatedCapabilitySubjectPermission.getPermissionResult()).isEqualTo(PermissionResult.ALLOWED);
    assertThat(updatedCapabilitySubjectPermission.getMaxValidUntil()).isGreaterThan(System.currentTimeMillis());
    assertThat(updatedCapabilitySubjectPermission.getRevalidateAfter()).isGreaterThan(System.currentTimeMillis());
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

  private CapabilityRequirement buildCapabilityRequirement(String capabilityId) {
    return CapabilityRequirement.builder()
        .accountId(delegate.getAccountId())
        .uuid(capabilityId)
        .maxValidityPeriod(100000)
        .revalidateAfterPeriod(50000)
        .capabilityType(CapabilityType.HTTP.name())
        .capabilityParameters(
            CapabilityParameters.newBuilder()
                .setHttpConnectionParameters(HttpConnectionParameters.newBuilder().setUrl("https://google.com").build())
                .build())
        .build();
  }
}
