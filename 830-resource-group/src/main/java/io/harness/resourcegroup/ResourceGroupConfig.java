package io.harness.resourcegroup;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.resourcegroup.model.ResourceSelector;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.PL)
@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@EqualsAndHashCode
public class ResourceGroupConfig {
  @NotNull @Size(max = 128) @NotEmpty String name;
  @NotNull @Size(max = 128) @NotEmpty String identifier;
  @Size(max = 1024) String description;
  @Size(max = 128) Map<String, String> tags = new HashMap<>();
  @Size(max = 256) @Valid List<ResourceSelector> resourceSelectors = new ArrayList<>();
  boolean fullScopeSelected;
  @NotEmpty Set<String> allowedScopeLevels = new HashSet<>();

  @Builder
  public ResourceGroupConfig(String name, String identifier, String description, Map<String, String> tags,
      List<ResourceSelector> resourceSelectors, boolean fullScopeSelected, Set<String> allowedScopeLevels) {
    this.name = name;
    this.identifier = identifier;
    this.description = description;
    this.tags = tags == null ? new HashMap<>() : tags;
    this.resourceSelectors = resourceSelectors == null ? new ArrayList<>() : resourceSelectors;
    this.fullScopeSelected = fullScopeSelected;
    this.allowedScopeLevels = allowedScopeLevels == null ? new HashSet<>() : allowedScopeLevels;
  }
}
