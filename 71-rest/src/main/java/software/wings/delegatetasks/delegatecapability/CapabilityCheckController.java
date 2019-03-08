package software.wings.delegatetasks.delegatecapability;

import static java.util.Collections.emptyList;

import com.google.inject.Inject;

import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.executioncapability.CapabilityCheckGeneratorFactory;
import io.harness.delegate.beans.executioncapability.CapabilityCheckResponse;
import io.harness.delegate.beans.executioncapability.CapabilityGenerator;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.executioncapability.CapabilityCheckFactory;
import software.wings.beans.DelegateTask;
import software.wings.delegatetasks.validation.AbstractDelegateValidateTask;
import software.wings.delegatetasks.validation.DelegateConnectionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CapabilityCheckController extends AbstractDelegateValidateTask {
  @Inject CapabilityCheckFactory capabilityCheckFactory;
  @Inject CapabilityCheckGeneratorFactory capabilityCheckGeneratorFactory;

  public CapabilityCheckController(
      String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
    super(delegateId, delegateTask, postExecute);
  }

  @Override
  public List<DelegateConnectionResult> validate() {
    List<CapabilityCheckResponse> checkResponses = new ArrayList<>();
    try {
      CapabilityGenerator capabilityGenerator =
          capabilityCheckGeneratorFactory.obtainCapabilityCheckGenerator(getTaskType());

      List<ExecutionCapability> delegateCapabilities =
          capabilityGenerator.generateDelegateCapabilities(getParameters());

      delegateCapabilities.forEach(delegateCapability
          -> checkResponses.add(capabilityCheckFactory.obtainCapabilityCheck(delegateCapability.fetchCapabilityType())
                                    .performCapabilityCheck(delegateCapability)));

    } catch (Exception e) {
      return emptyList();
    }

    return convertResponsesIntoDelegateConnectionResults(checkResponses);
  }

  @Override
  public List<String> getCriteria() {
    CapabilityGenerator checkGenerator = capabilityCheckGeneratorFactory.obtainCapabilityCheckGenerator(getTaskType());

    List<ExecutionCapability> delegateCapabilities = checkGenerator.generateDelegateCapabilities(getParameters());

    List<String> criterias = new ArrayList<>();
    delegateCapabilities.forEach(delegateCapability -> criterias.add(delegateCapability.fetchCapabilityBasis()));
    return criterias;
  }

  // TODO: remove this method once manager is updated to receive CapabilityCheckResponse
  // Manager expects DelegateConnectionResult. This is to be deprecated in future,
  // So we receive output with new data structure "CapabilityCheckResponse"
  // and convert in into something manager understands for now
  private List<DelegateConnectionResult> convertResponsesIntoDelegateConnectionResults(
      List<CapabilityCheckResponse> checkResponses) {
    List<DelegateConnectionResult> delegateConnectionResults = new ArrayList<>();

    if (EmptyPredicate.isNotEmpty(checkResponses)) {
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
