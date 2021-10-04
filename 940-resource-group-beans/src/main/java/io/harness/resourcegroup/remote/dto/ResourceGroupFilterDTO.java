package io.harness.resourcegroup.remote.dto;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.validator.ValidResourceGroupFilter;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.HashSet;
import java.util.Set;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@ValidResourceGroupFilter
public class ResourceGroupFilterDTO {
  @ApiModelProperty(required = true) String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  String searchTerm;
  Set<String> identifierFilter;
  @JsonIgnore Set<String> scopeLevelFilter;
  Set<ResourceSelectorFilter> resourceSelectorFilterList;
  ManagedFilter managedFilter;

  @Builder
  public ResourceGroupFilterDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      String searchTerm, Set<String> identifierFilter, ManagedFilter managedFilter,
      Set<ResourceSelectorFilter> resourceSelectorFilterList) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.searchTerm = searchTerm;
    this.identifierFilter = identifierFilter == null ? new HashSet<>() : identifierFilter;
    this.managedFilter = managedFilter == null ? ManagedFilter.NO_FILTER : managedFilter;
    this.resourceSelectorFilterList = resourceSelectorFilterList == null ? new HashSet<>() : resourceSelectorFilterList;
  }
}
