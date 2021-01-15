package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.beans.ConstructorProperties;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@JsonTypeName("useFromStage")
@TypeAlias("useFromStageInfraYaml")
public class UseFromStageInfraYaml implements Infrastructure {
  @JsonIgnore private Type type;
  private String useFromStage;

  @Builder
  @ConstructorProperties({"type", "useFromStage"})
  public UseFromStageInfraYaml(Type type, String useFromStage) {
    this.useFromStage = useFromStage;
    this.type = Type.USE_FROM_STAGE;
  }
}
