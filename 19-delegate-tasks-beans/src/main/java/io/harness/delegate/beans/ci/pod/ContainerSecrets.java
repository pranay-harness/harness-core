package io.harness.delegate.beans.ci.pod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContainerSecrets {
  @Builder.Default private List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
  @Builder.Default private Map<String, ConnectorDetails> publishArtifactConnectors = new HashMap<>();
}
