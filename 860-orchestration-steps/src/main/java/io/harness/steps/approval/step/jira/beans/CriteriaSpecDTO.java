package io.harness.steps.approval.step.jira.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@OwnedBy(CDC)
@JsonSubTypes({
  @JsonSubTypes.Type(value = JexlCriteriaSpecDTO.class, name = "Jexl")
  , @JsonSubTypes.Type(value = KeyValuesCriteriaSpecDTO.class, name = "KeyValues")
})
public interface CriteriaSpecDTO {}
