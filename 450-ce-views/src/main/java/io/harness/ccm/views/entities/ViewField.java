/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.views.entities;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ViewField {
  private String fieldId;
  private String fieldName;
  private ViewFieldIdentifier identifier;
  private String identifierName;
}
