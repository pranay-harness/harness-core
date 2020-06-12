package io.harness.cvng.core.services.api;

import io.harness.cvng.beans.DSConfig;
import io.harness.cvng.core.services.entities.CVConfig;

import java.util.List;

public interface CVConfigTransformer<C extends CVConfig, T extends DSConfig> {
  default T transform(List<? extends CVConfig> cvConfigGroup) {
    List<C> typedCVConfig = (List<C>) cvConfigGroup;
    return transformToDSConfig(typedCVConfig);
  }
  T transformToDSConfig(List<C> cvConfigGroup);
}
