package io.harness.cvng.core.services.api;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.entities.CVConfig;

import com.google.common.base.Preconditions;
import java.util.List;

public interface CVConfigTransformer<C extends CVConfig, T extends DSConfig> {
  default T transform(List<? extends CVConfig> cvConfigGroup) {
    Preconditions.checkArgument(isNotEmpty(cvConfigGroup), "List of cvConfigs can not empty");
    Preconditions.checkArgument(cvConfigGroup.stream().map(CVConfig::getGroupId).distinct().count() == 1,
        "Group ID should be same for List of all configs.");
    List<C> typedCVConfig = (List<C>) cvConfigGroup;
    return transformToDSConfig(typedCVConfig);
  }
  T transformToDSConfig(List<C> cvConfigGroup);
}
