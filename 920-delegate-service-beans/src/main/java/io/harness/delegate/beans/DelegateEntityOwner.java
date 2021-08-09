package io.harness.delegate.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@FieldNameConstants(innerTypeName = "DelegateEntityOwnerKeys")
@Data
@Builder
@OwnedBy(HarnessTeam.DEL)
public class DelegateEntityOwner {
  @NotEmpty private String identifier;
}
