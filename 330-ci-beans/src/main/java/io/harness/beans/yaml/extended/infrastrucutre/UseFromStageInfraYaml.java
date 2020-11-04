package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeName("useFromStage")
public class UseFromStageInfraYaml implements Infrastructure {
  @JsonIgnore @Builder.Default private Type type = Type.USE_FROM_STAGE;
  private UseFromStage useFromStage;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UseFromStage {
    private String stage;
  }
}
