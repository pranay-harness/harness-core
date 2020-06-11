package io.harness.beans.steps;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.harness.state.io.StepParameters;
import io.harness.yaml.core.intfc.StepInfo;
import io.harness.yaml.core.nonyaml.WithNonYamlInfo;

import java.util.List;

@JsonDeserialize
public interface CIStepInfo extends StepInfo, StepParameters, WithNonYamlInfo<TypeInfo> {
  int MIN_RETRY = 0;
  int MAX_RETRY = 5;
  int MIN_TIMEOUT = 1;
  int MAX_TIMEOUT = 999;

  String getName();

  int getRetry();
  int getTimeout();

  // TODO: implement this when we support graph section in yaml
  default List<String> getDependencies() {
    return null;
  }
}
