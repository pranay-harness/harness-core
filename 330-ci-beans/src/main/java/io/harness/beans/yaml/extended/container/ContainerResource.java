package io.harness.beans.yaml.extended.container;

import io.harness.beans.yaml.extended.container.quantity.CpuQuantity;
import io.harness.beans.yaml.extended.container.quantity.MemoryQuantity;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @Min(0) private MemoryQuantity memory;
    @Min(0) private CpuQuantity cpu;

    @Builder
    @JsonCreator
    public Limits(@JsonProperty("memory") MemoryQuantity memory, @JsonProperty("cpu") CpuQuantity cpu) {
      this.memory = memory;
      this.cpu = cpu;
    }
  }
}
