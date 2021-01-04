package io.harness.delegate.task.executioncapability;

import static io.harness.capability.CapabilitySubjectPermission.PermissionResult;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;

import software.wings.delegatetasks.delegatecapability.CapabilityCheckFactory;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
public class BatchCapabilityCheckTask extends AbstractDelegateRunnableTask {
  @Inject CapabilityCheckFactory capabilityCheckFactory;

  public BatchCapabilityCheckTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("deprecated");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    List<CapabilityCheckDetails> capabilityCheckDetailsResultsList = new ArrayList<>();

    List<CapabilityCheckDetails> capabilityCheckDetailsList =
        ((BatchCapabilityCheckTaskParameters) parameters).getCapabilityCheckDetailsList();

    for (CapabilityCheckDetails capabilityCheckDetails : capabilityCheckDetailsList) {
      CapabilityCheck capabilityCheck =
          capabilityCheckFactory.obtainCapabilityCheck(capabilityCheckDetails.getCapabilityType());

      if (capabilityCheck == null) {
        log.error("Unknown capability type: {}", capabilityCheckDetails.getCapabilityType());
        capabilityCheckDetailsResultsList.add(
            capabilityCheckDetails.toBuilder().permissionResult(PermissionResult.UNCHECKED).build());
        continue;
      }
      //      TODO: uncomment when method performCapabilityCheckWithProto is added to the interface
      //      CapabilitySubjectPermission checkResult =
      //          capabilityCheck.performCapabilityCheckWithProto(capabilityCheckDetails.getCapabilityParameters());
      // capabilityCheckDetailsResultsList.add(
      //      capabilityCheckDetails.toBuilder().permissionResult(checkResult.getPermissionResult()).build());
    }

    return BatchCapabilityCheckTaskResponse.builder()
        .capabilityCheckDetailsList(capabilityCheckDetailsResultsList)
        .build();
  }
}
