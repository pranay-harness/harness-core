/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.walktree.visitor.mergeinputset.beans;

import io.harness.walktree.visitor.response.VisitorErrorResponse;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@EqualsAndHashCode(callSuper = true)
public class MergeInputSetErrorResponse extends VisitorErrorResponse {
  String identifierOfErrorSource;

  @Builder(builderMethodName = "mergeErrorBuilder")
  public MergeInputSetErrorResponse(String fieldName, String message, String identifierOfErrorSource) {
    super(fieldName, message);
    this.identifierOfErrorSource = identifierOfErrorSource;
  }
}
