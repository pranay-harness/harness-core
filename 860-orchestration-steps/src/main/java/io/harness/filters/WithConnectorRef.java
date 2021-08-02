package io.harness.filters;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.ParameterField;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
@TargetModule(HarnessModule._878_PIPELINE_SERVICE_UTILITIES)
public interface WithConnectorRef {
  /**
   *
   * @return a map of relative fqn from step to the connector ref parameter field value
   */
  Map<String, ParameterField<String>> extractConnectorRefs();
}
