/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CI)
public class ContainerSecrets {
  @Builder.Default private List<SecretVariableDetails> secretVariableDetails = new ArrayList<>();
  @Builder.Default private Map<String, ConnectorDetails> connectorDetailsMap = new HashMap<>();
  @Builder.Default private Map<String, ConnectorDetails> functorConnectors = new HashMap<>();
  @Builder.Default private Map<String, SecretParams> plainTextSecretsByName = new HashMap<>();
}
