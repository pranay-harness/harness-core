package software.wings.delegatetasks.delegatecapability;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.stream.Collectors.toList;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.delegate.beans.executioncapability.CapabilityResponse;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.executioncapability.CapabilityCheck;
import lombok.extern.slf4j.Slf4j;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
public class CapabilityCheckController extends AbstractDelegateValidateTask {
  @Inject CapabilityCheckFactory capabilityCheckFactory;

  public CapabilityCheckController(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    List<CapabilityResponse> checkResponses = new ArrayList<>();
    try {
      List<ExecutionCapability> executionCapabilities = getExecutionCapabilities();

      if (isEmpty(executionCapabilities)) {
        executionCapabilities =
            Arrays.stream(getParameters())
                .filter(param -> param instanceof ExecutionCapabilityDemander)
                .flatMap(param -> ((ExecutionCapabilityDemander) param).fetchRequiredExecutionCapabilities().stream())
                .collect(toList());
      }

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

    } catch (RuntimeException exception) {
      logger.error("Exception while evaluating capabilities", exception);
      return null;
    }

    return convertResponsesIntoDelegateConnectionResults(checkResponses);
  }

  @Override
  public List<String> getCriteria() {
    List<ExecutionCapability> executionCapabilities = getExecutionCapabilities();

    if (isEmpty(executionCapabilities)) {
      executionCapabilities =
          Arrays.stream(getParameters())
              .filter(param -> param instanceof ExecutionCapabilityDemander)
              .flatMap(param -> ((ExecutionCapabilityDemander) param).fetchRequiredExecutionCapabilities().stream())
              .collect(toList());
    }

    return executionCapabilities.stream().map(ExecutionCapability::fetchCapabilityBasis).collect(toList());
  }

  // TODO: remove this method once manager is updated to receive CapabilityResponse
  // Manager expects DelegateConnectionResult. This is to be deprecated in future,
  // So we receive output with new data structure "CapabilityResponse"
  // and convert in into something manager understands for now
  private List<DelegateConnectionResult> convertResponsesIntoDelegateConnectionResults(
      List<CapabilityResponse> checkResponses) {
    List<DelegateConnectionResult> delegateConnectionResults = new ArrayList<>();

    if (isNotEmpty(checkResponses)) {
      checkResponses.forEach(checkResponse -> {
        delegateConnectionResults.add(DelegateConnectionResult.builder()
                                          .validated(checkResponse.isValidated())
                                          .criteria(checkResponse.getDelegateCapability().fetchCapabilityBasis())
                                          .build());
      });
    }

    return delegateConnectionResults;
  }
}
