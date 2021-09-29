package io.harness.resourcegroup.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.swagger.annotations.ApiModelProperty;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@OwnedBy(PL)
@Value
@Builder
public class ResourceGroupFilterDTO {
  @ApiModelProperty(required = true) String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String searchTerm;
  Set<String> identifierFilter;
}
