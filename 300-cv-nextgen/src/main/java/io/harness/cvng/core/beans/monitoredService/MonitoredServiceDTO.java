package io.harness.cvng.core.beans.monitoredService;

import io.harness.cvng.beans.MonitoredServiceType;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class MonitoredServiceDTO implements YamlDTO {
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String orgIdentifier;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String projectIdentifier;
  @ApiModelProperty(required = true) @NotNull String identifier;
  @ApiModelProperty(required = true) @NotNull @NGEntityName String name;
  @ApiModelProperty(required = true) @NotNull MonitoredServiceType type;
  String description;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String serviceRef;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String environmentRef;
  @ApiModelProperty(required = true) @NotNull @Size(max = 128) Map<String, String> tags;
  @Valid Sources sources;
  @Valid Set<ServiceRef> dependencies;

  @Data
  @Builder
  public static class ServiceRef {
    @EntityIdentifier String serviceRef;
  }

  public Set<ServiceRef> getDependencies() {
    if (dependencies == null) {
      return new HashSet<>();
    }
    return dependencies;
  }

  @Data
  @Builder
  public static class Sources {
    @Valid Set<HealthSource> healthSources;
    @Valid Set<ChangeSourceDTO> changeSources;

    public Sources(Set<HealthSource> healthSources, Set<ChangeSourceDTO> changeSources) {
      this.healthSources = healthSources == null ? Collections.EMPTY_SET : healthSources;
      this.changeSources = changeSources == null ? Collections.EMPTY_SET : changeSources;
    }
  }
}
