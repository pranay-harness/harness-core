/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
