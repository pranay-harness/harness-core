package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@FieldNameConstants(innerTypeName = "DelegateEntityOwnerKeys")
@Data
@Builder
public class DelegateEntityOwner {
  @NotEmpty private String identifier;
}
