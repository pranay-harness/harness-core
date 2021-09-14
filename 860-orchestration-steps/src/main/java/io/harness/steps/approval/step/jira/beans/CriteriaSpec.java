/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@OwnedBy(CDC)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = JexlCriteriaSpec.class, name = CriteriaSpecTypeConstants.JEXL)
      , @JsonSubTypes.Type(value = KeyValuesCriteriaSpec.class, name = CriteriaSpecTypeConstants.KEY_VALUES)
    })
public interface CriteriaSpec {
  @JsonIgnore CriteriaSpecType getType();
  @JsonIgnore CriteriaSpecDTO toCriteriaSpecDTO(boolean skipEmpty);
}
