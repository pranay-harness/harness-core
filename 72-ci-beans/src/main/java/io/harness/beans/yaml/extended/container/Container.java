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
  public static final int MEM_RESERVE_DEFAULT = 9000;
  public static final int MEM_LIMIT_DEFAULT = 9000;
  public static final int CPU_MILLI_RESERVE_DEFAULT = 2000;
  public static final int CPU_MILLI_LIMIT_DEFAULT = 2000;

  @NotNull private String identifier;
  @NotNull private String connector;
  @NotNull private String imagePath;
  @NotNull @Builder.Default Resources resources = Resources.builder().build();

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Resources {
    @Builder.Default Limit limit = Limit.builder().build();
    @Builder.Default Reserve reserve = Reserve.builder().build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Limit {
    @Builder.Default @Min(0) private int memory = MEM_LIMIT_DEFAULT;
    @Builder.Default @Min(0) private int cpu = CPU_MILLI_LIMIT_DEFAULT;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Reserve {
    @Builder.Default @Min(0) private int memory = MEM_RESERVE_DEFAULT;
    @Builder.Default @Min(0) private int cpu = CPU_MILLI_RESERVE_DEFAULT;
  }
}
