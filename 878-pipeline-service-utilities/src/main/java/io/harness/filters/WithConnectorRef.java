package io.harness.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public interface WithConnectorRef {
  /**
   *
   * @return a map of relative fqn from step to the connector ref parameter field value
   */
  Map<String, ParameterField<String>> extractConnectorRefs();
}
