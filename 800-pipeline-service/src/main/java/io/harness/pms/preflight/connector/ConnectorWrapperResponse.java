/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.preflight.connector;

import io.harness.pms.preflight.PreFlightStatus;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorWrapperResponse {
  List<ConnectorCheckResponse> checkResponses;
  PreFlightStatus status;
  String label;
}
