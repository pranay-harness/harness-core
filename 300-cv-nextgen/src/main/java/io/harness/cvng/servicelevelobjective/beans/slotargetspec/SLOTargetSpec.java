package io.harness.cvng.servicelevelobjective.beans.slotargetspec;

import io.harness.cvng.servicelevelobjective.beans.SLOTargetType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSubTypes({
  @JsonSubTypes.Type(value = RollingSLOTargetSpec.class, name = "Rolling")
  , @JsonSubTypes.Type(value = CalenderSLOTargetSpec.class, name = "Calender"),
})
public abstract class SLOTargetSpec {
  @JsonIgnore public abstract SLOTargetType getType();
}