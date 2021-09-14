/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ngtriggers.eventmapper;

import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.scm.GitConnectionType;
import io.harness.ngtriggers.beans.dto.TriggerDetails;

import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TriggerGitConnectorWrapper {
  ConnectorConfigDTO connectorConfigDTO;
  ConnectorType connectorType;
  GitConnectionType gitConnectionType;
  String url;
  List<TriggerDetails> triggers;
  String connectorFQN;
}
