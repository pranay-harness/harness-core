/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.response;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class VisitorErrorResponse {
  String fieldName;
  String message;

  @Builder(builderMethodName = "errorBuilder")
  public VisitorErrorResponse(String fieldName, String message) {
    this.fieldName = fieldName;
    this.message = message;
  }
}
