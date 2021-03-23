package io.harness.steps.approval.step.jira.beans;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
@JsonSubTypes(value =
    {
      @JsonSubTypes.Type(value = JexlCriteriaSpec.class, name = "Jexl")
      , @JsonSubTypes.Type(value = KeyValuesCriteriaSpec.class, name = "KeyValues")
    })

public interface CriteriaSpec {
  @JsonIgnore CriteriaSpecType getType();
}
