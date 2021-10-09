package io.harness.resourcegroup.remote.dto;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.resourcegroup.model.DynamicResourceSelector;
import io.harness.resourcegroup.model.ResourceSelector;
import io.harness.resourcegroup.model.StaticResourceSelector;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(HarnessTeam.PL)
public class ResourceGroupDTO {
  @ApiModelProperty(required = true) @NotNull @NotEmpty String accountIdentifier;
  String orgIdentifier;
  String projectIdentifier;
  @EntityIdentifier @ApiModelProperty(required = true) @NotNull @Size(max = 128) @NotEmpty String identifier;
  @NGEntityName @ApiModelProperty(required = true) @NotNull @Size(max = 128) @NotEmpty String name;
  @Size(max = 256) @Valid List<ResourceSelector> resourceSelectors;
  boolean fullScopeSelected;
  @Size(max = 128) Map<String, String> tags;
  @Size(max = 1024) String description;
  @JsonIgnore Set<String> allowedScopeLevels;
  String color;

  @Builder
  public ResourceGroupDTO(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String name, List<ResourceSelector> resourceSelectors, boolean fullScopeSelected, Map<String, String> tags,
      String description, String color) {
    this.accountIdentifier = accountIdentifier;
    this.orgIdentifier = orgIdentifier;
    this.projectIdentifier = projectIdentifier;
    this.identifier = identifier;
    this.name = name;
    this.resourceSelectors = resourceSelectors;
    this.fullScopeSelected = fullScopeSelected;
    this.tags = tags;
    this.description = description;
    this.color = color;
  }

  public Scope getScope() {
    return Scope.of(accountIdentifier, orgIdentifier, projectIdentifier);
  }

  public void sanitizeResourceSelectors() {
    if (getResourceSelectors() == null) {
      return;
    }
    List<ResourceSelector> sanitizedResourceSelectors = getResourceSelectors()
                                                            .stream()
                                                            .filter(DynamicResourceSelector.class ::isInstance)
                                                            .map(DynamicResourceSelector.class ::cast)
                                                            .distinct()
                                                            .collect(toList());

    Map<String, List<String>> staticResources =
        getResourceSelectors()
            .stream()
            .filter(StaticResourceSelector.class ::isInstance)
            .map(StaticResourceSelector.class ::cast)
            .collect(toMap(StaticResourceSelector::getResourceType, StaticResourceSelector::getIdentifiers,
                (oldResourceIds, newResourceIds) -> {
                  oldResourceIds.addAll(newResourceIds);
                  return oldResourceIds.stream().distinct().collect(toList());
                }));

    staticResources.forEach(
        (resourceType, identifiers)
            -> sanitizedResourceSelectors.add(
                StaticResourceSelector.builder().resourceType(resourceType).identifiers(identifiers).build()));

    resourceSelectors = sanitizedResourceSelectors;
  }
}
