package io.harness.pms.cdng.infra.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.cdng.environment.yaml.EnvironmentYamlPms;
import io.harness.pms.cdng.infra.InfraStructureDefPms;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("infraUseFromStage")
public class InfraUseFromStagePms implements Serializable {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  // Stage identifier of the stage to select from.
  @NotNull String stage;
  Overrides overrides;

  @Data
  @Builder
  @ApiModel(value = "InfraOverrides")
  @TypeAlias("infraUseFromStage_overrides")
  public static class Overrides implements Serializable {
    EnvironmentYamlPms environmentPms;
    InfraStructureDefPms infraStructureDefPms;
  }
}
