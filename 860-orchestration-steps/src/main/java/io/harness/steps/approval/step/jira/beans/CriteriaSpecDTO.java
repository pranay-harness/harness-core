/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = JexlCriteriaSpecDTO.class, name = CriteriaSpecTypeConstants.JEXL)
  , @JsonSubTypes.Type(value = KeyValuesCriteriaSpecDTO.class, name = CriteriaSpecTypeConstants.KEY_VALUES)
})
public interface CriteriaSpecDTO {
  @JsonIgnore boolean isEmpty();
}
