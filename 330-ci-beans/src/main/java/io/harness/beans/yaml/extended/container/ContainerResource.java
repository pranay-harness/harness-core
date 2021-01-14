package io.harness.beans.yaml.extended.container;

import io.harness.common.SwaggerConstants;
import io.harness.pms.yaml.ParameterField;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@Data
@TypeAlias("resource")
public class ContainerResource {
  @NotNull Limits limits;

  @Builder
  @JsonCreator
  public ContainerResource(@JsonProperty("limits") Limits limits) {
    this.limits = Optional.ofNullable(limits).orElse(Limits.builder().build());
  }

  @Data
  @TypeAlias("resource_limits")
  public static class Limits {
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Min(0) private ParameterField<String> memory;
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Min(0) private ParameterField<String> cpu;

    @Builder
    @JsonCreator
    public Limits(
        @JsonProperty("memory") ParameterField<String> memory, @JsonProperty("cpu") ParameterField<String> cpu) {
      this.memory = memory;
      this.cpu = cpu;
    }
  }
}
