package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("useFromStage")
@TypeAlias("useFromStageInfraYaml")
public class UseFromStageInfraYaml implements Infrastructure {
  @JsonIgnore @Builder.Default private Type type = Type.USE_FROM_STAGE;
  private UseFromStage useFromStage;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @TypeAlias("useFromStageInfraYaml_useFromStage")
  public static class UseFromStage {
    private String stage;
  }
}
