/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@Builder
@FieldNameConstants(innerTypeName = "ScopingRuleDetailsKeys")
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
public class ScopingRuleDetails {
  private String description;

  private String applicationId;
  private Set<String> serviceIds;
  private String environmentTypeId;
  private Set<String> environmentIds;
}
