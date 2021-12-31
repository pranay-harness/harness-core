package io.harness.steps.approval.step.beans;

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
