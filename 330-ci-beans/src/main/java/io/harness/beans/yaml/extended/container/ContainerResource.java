package io.harness.beans.yaml.extended.container;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
public class ContainerResource {
  public static final int MEM_LIMIT_DEFAULT = 200;
  public static final int CPU_MILLI_LIMIT_DEFAULT = 200;

  @NotNull Limit limit;

  @Builder
  @ConstructorProperties({"limit"})
  public ContainerResource(Limit limit) {
    this.limit = Optional.ofNullable(limit).orElse(Limit.builder().build());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Limit {
    @Builder.Default @Min(0) private int memory = MEM_LIMIT_DEFAULT;
    @Builder.Default @Min(0) private int cpu = CPU_MILLI_LIMIT_DEFAULT;
  }
}
