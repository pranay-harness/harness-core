/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import io.harness.ng.core.dto.ErrorDetail;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ConnectorConnectivityDetailsKeys")
public class ConnectorConnectivityDetails {
  ConnectivityStatus status;
  String errorSummary;
  List<ErrorDetail> errors;
  long testedAt;
  @Deprecated long lastTestedAt;
  long lastConnectedAt;
}
