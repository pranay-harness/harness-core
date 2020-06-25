package io.harness.state.io;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Value
@Builder
public class StepInputPackage {
  @Singular List<ResolvedRefInput> inputs;
  @Singular List<StepTransput> additionalInputs;

  public List<StepTransput> findByRefKey(String refKey) {
    if (isEmpty(inputs)) {
      return Collections.emptyList();
    }
    return inputs.stream()
        .filter(resolvedRefInput -> resolvedRefInput.getRefObject().getKey().equals(refKey))
        .map(ResolvedRefInput::getTransput)
        .collect(Collectors.toList());
  }
}
