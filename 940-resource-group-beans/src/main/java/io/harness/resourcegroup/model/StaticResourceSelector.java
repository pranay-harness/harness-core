package io.harness.resourcegroup.model;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@Builder
@FieldNameConstants(innerTypeName = "StaticResourceSelectorKeys")
@OwnedBy(HarnessTeam.PL)
public class StaticResourceSelector implements ResourceSelector {
  @NotNull String resourceType;
  @NotEmpty List<String> identifiers;
}
