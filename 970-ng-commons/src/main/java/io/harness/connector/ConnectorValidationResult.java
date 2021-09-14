/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.connector;

import io.harness.ng.core.dto.ErrorDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ConnectorValidationResult {
  ConnectivityStatus status;
  List<ErrorDetail> errors;
  String errorSummary;
  long testedAt;
  String delegateId;
}
