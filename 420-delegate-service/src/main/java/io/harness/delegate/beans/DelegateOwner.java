package io.harness.delegate.beans;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@FieldNameConstants(innerTypeName = "DelegateOwnerKeys")
@Data
@Builder
public class DelegateOwner {
  @NotEmpty private String entityType;
  @NotEmpty private String entityId;
}
