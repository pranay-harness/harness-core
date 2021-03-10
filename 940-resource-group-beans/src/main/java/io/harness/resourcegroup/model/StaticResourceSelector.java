package io.harness.resourcegroup.model;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldNameConstants(innerTypeName = "StaticResourceSelectorKeys")
public class StaticResourceSelector implements ResourceSelector {
  @NotEmpty List<String> identifiers;
  @NotNull String resourceType;
}
