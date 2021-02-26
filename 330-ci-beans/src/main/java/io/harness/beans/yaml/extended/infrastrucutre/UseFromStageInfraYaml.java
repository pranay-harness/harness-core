package io.harness.beans.yaml.extended.infrastrucutre;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@JsonTypeName("useFromStage")
@TypeAlias("useFromStageInfraYaml")
public class UseFromStageInfraYaml implements Infrastructure {
  @NotNull private String useFromStage;

  @JsonIgnore
  @Override
  public Type getType() {
    return Type.USE_FROM_STAGE;
  }
}
