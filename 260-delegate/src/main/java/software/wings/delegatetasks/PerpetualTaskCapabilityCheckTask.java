package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.PerpetualTaskCapabilityCheckTaskParameters;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.executioncapability.CapabilityCheck;

import software.wings.delegatetasks.delegatecapability.CapabilityCheckFactory;
import software.wings.service.impl.CapabilityCheckResponse;
import software.wings.service.impl.PerpetualTaskCapabilityCheckResponse;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.DEL)
public class PerpetualTaskCapabilityCheckTask extends AbstractDelegateRunnableTask {
  @Inject CapabilityCheckFactory capabilityCheckFactory;

  public PerpetualTaskCapabilityCheckTask(DelegateTaskPackage delegateTaskPackage,
      ILogStreamingTaskClient logStreamingTaskClient, Consumer<DelegateTaskResponse> consumer,
      BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public CapabilityCheckResponse run(TaskParameters parameters) {
    PerpetualTaskCapabilityCheckTaskParameters perpetualTaskCapabilityCheckTaskParameters =
        (PerpetualTaskCapabilityCheckTaskParameters) parameters;
    List<CapabilityResponse> checkResponses = new ArrayList<>();

    List<ExecutionCapability> executionCapabilities =
        perpetualTaskCapabilityCheckTaskParameters.getExecutionCapabilityList();

    if (isEmpty(executionCapabilities)) {
      return PerpetualTaskCapabilityCheckResponse.builder().ableToExecutePerpetualTask(true).build();
    }

    executionCapabilities.forEach(delegateCapability -> {
      log.info("Checking Capability: " + delegateCapability.toString());
      CapabilityCheck capabilityCheck =
          capabilityCheckFactory.obtainCapabilityCheck(delegateCapability.getCapabilityType());

      if (capabilityCheck == null) {
        log.error("Unknown capability type: {}", delegateCapability.getCapabilityType());
        return;
      }

      checkResponses.add(capabilityCheck.performCapabilityCheck(delegateCapability));
    });
    boolean validated = false;
    if (isNotEmpty(checkResponses)) {
      validated = checkResponses.stream().allMatch(CapabilityResponse::isValidated);
    }
    return PerpetualTaskCapabilityCheckResponse.builder().ableToExecutePerpetualTask(validated).build();
  }

  @Override
  public CapabilityCheckResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }
}
