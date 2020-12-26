package io.harness.beans.steps;

import io.harness.executionplan.plancreator.beans.GenericStepInfo;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.time.Duration;
import java.util.List;

@JsonDeserialize
public interface CIStepInfo extends StepSpecType, GenericStepInfo {
  int MIN_RETRY = 0;
  int MAX_RETRY = 5;
  long DEFAULT_TIMEOUT = Duration.ofHours(2).getSeconds();

  TypeInfo getNonYamlInfo();
  int getRetry();
  String getName();

  default long getDefaultTimeout() {
    return DEFAULT_TIMEOUT;
  }

  // TODO: implement this when we support graph section in yaml
  default List<String> getDependencies() {
    return null;
  }
}
