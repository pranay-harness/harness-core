package io.harness.beans.yaml.extended.container;

import io.harness.yaml.core.intfc.WithIdentifier;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Container implements WithIdentifier {
  public static final int MEM_REQ_DEFAULT = 900;
  public static final int MEM_LIMIT_DEFAULT = 900;
  public static final int CPU_MILLI_REQ_DEFAULT = 1200;
  public static final int CPU_MILLI_LIMIT_DEFAULT = 1200;

  @NotNull private String identifier;
  @NotNull private String connector;
  @NotNull private String imagePath;
  @NotNull @Builder.Default Resources resources = Resources.builder().build();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Resources {
    @Builder.Default @Min(0) private int requestMemoryMiB = MEM_REQ_DEFAULT;
    @Builder.Default @Min(0) private int limitMemoryMiB = MEM_LIMIT_DEFAULT;
    @Builder.Default @Min(0) private int requestMilliCPU = CPU_MILLI_REQ_DEFAULT;
    @Builder.Default @Min(0) private int limitMilliCPU = CPU_MILLI_LIMIT_DEFAULT;
  }
}
