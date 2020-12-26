package io.harness.beans.yaml.extended.container;

import io.harness.beans.yaml.extended.container.quantity.CpuQuantity;
import io.harness.beans.yaml.extended.container.quantity.MemoryQuantity;

import java.beans.ConstructorProperties;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@TypeAlias("resource")
public class ContainerResource {
  public static final String MEM_LIMIT_DEFAULT = "200Mi";
  public static final String CPU_MILLI_LIMIT_DEFAULT = "200m";

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
  @TypeAlias("resource_limit")
  public static class Limit {
    @Builder.Default @Min(0) private MemoryQuantity memory = MemoryQuantity.fromString(MEM_LIMIT_DEFAULT);
    @Builder.Default @Min(0) private CpuQuantity cpu = CpuQuantity.fromString(CPU_MILLI_LIMIT_DEFAULT);
  }
}
