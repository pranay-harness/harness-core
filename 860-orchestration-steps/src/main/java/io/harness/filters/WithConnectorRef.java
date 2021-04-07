package io.harness.filters;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;

import java.util.List;

@OwnedBy(HarnessTeam.PIPELINE)
public interface WithConnectorRef {
  List<ParameterField<String>> extractConnectorRefs();
}
