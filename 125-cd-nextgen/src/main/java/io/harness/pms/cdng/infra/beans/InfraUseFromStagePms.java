package io.harness.pms.cdng.infra.beans;

import io.harness.pms.cdng.environment.yaml.EnvironmentYamlPms;
import io.harness.pms.cdng.infra.InfraStructureDefPms;

import io.swagger.annotations.ApiModel;
import java.io.Serializable;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("infraUseFromStage")
public class InfraUseFromStagePms implements Serializable {
  // Stage identifier of the stage to select from.
  String uuid;
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
