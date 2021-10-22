package io.harness.cvng.servicelevelobjective.beans;

import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.NGEntityName;
import io.harness.gitsync.beans.YamlDTO;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;
import java.util.Map;
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
public class ServiceLevelObjectiveDTO implements YamlDTO {
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String orgIdentifier;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String projectIdentifier;
  @ApiModelProperty(required = true) @NotNull @EntityIdentifier String identifier;
  @ApiModelProperty(required = true) @NotNull @NGEntityName String name;
  String description;
  @ApiModelProperty(required = true) @NotNull @Size(max = 128) Map<String, String> tags;
  @ApiModelProperty(required = true) @NotNull String userJourneyRef;
  @ApiModelProperty(required = true) @NotNull String monitoredServiceRef;
  @ApiModelProperty(required = true) @NotNull String healthSourceRef;

  @Valid @NotNull List<ServiceLevelIndicator> serviceLevelIndicators;
  @Valid @NotNull SLOTarget target;
}
