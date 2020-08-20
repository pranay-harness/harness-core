package software.wings.delegatetasks;

import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.delegatetasks.delegatecapability.CapabilityCheckFactory;
import software.wings.service.impl.CapabilityCheckResponse;
import software.wings.service.impl.PerpetualTaskCapabilityCheckResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class PerpetualTaskCapabilityCheckTask extends AbstractDelegateRunnableTask {
  @Inject CapabilityCheckFactory capabilityCheckFactory;

  public PerpetualTaskCapabilityCheckTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public CapabilityCheckResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public CapabilityCheckResponse run(Object[] parameters) {
    List<CapabilityResponse> checkResponses = new ArrayList<>();

    List<ExecutionCapability> executionCapabilities =
        Arrays.stream(parameters).map(param -> (ExecutionCapability) param).collect(toList());

    executionCapabilities.forEach(delegateCapability -> {
      logger.info("Checking Capability: " + delegateCapability.toString());
      CapabilityCheck capabilityCheck =
          capabilityCheckFactory.obtainCapabilityCheck(delegateCapability.getCapabilityType());

      if (capabilityCheck == null) {
        logger.error("Unknown capability type: {}", delegateCapability.getCapabilityType());
        return;
      }

      checkResponses.add(capabilityCheck.performCapabilityCheck(delegateCapability));
    });
    boolean validated = checkResponses.stream().anyMatch(CapabilityResponse::isValidated);
    return PerpetualTaskCapabilityCheckResponse.builder().ableToExecutePerpetualTask(validated).build();
  }
}
