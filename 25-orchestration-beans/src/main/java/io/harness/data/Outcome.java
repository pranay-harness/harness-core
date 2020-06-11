package io.harness.data;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.references.RefType;
import io.harness.state.io.StepTransput;

import java.io.Serializable;

@OwnedBy(CDC)
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "class") // TODO JsonTypeInfo remove
public interface Outcome extends StepTransput, Serializable {
  @Override
  default RefType getRefType() {
    return RefType.builder().type(RefType.OUTCOME).build();
  }
}
