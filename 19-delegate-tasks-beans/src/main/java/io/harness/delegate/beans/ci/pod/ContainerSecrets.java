package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ContainerSecrets {
  @Builder.Default private List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
  @Builder.Default private Map<String, ConnectorDetails> publishArtifactConnectors = new HashMap<>();
}
