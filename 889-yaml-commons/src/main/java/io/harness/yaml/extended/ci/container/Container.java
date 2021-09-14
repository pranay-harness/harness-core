/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.yaml.extended.ci.container;

import io.harness.beans.WithIdentifier;

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
@TypeAlias("container")
public class Container implements WithIdentifier {
  public static final int MEM_RESERVE_DEFAULT = 9000;
  public static final int MEM_LIMIT_DEFAULT = 9000;
  public static final int CPU_MILLI_RESERVE_DEFAULT = 2000;
  public static final int CPU_MILLI_LIMIT_DEFAULT = 2000;

  @NotNull private String identifier;
  @NotNull private String connector;
  @NotNull private String imagePath;
  @NotNull Resources resources;

  @Builder
  @ConstructorProperties({"identifier", "connector", "imagePath", "resources"})
  public Container(String identifier, String connector, String imagePath, Resources resources) {
    this.identifier = identifier;
    this.connector = connector;
    this.imagePath = imagePath;
    this.resources = Optional.ofNullable(resources).orElse(Resources.builder().build());
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @TypeAlias("container_resources")
  public static class Resources {
    @Builder.Default Limit limit = Limit.builder().build();
    @Builder.Default Reserve reserve = Reserve.builder().build();
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @TypeAlias("container_limit")
  public static class Limit {
    @Builder.Default @Min(0) private int memory = MEM_LIMIT_DEFAULT;
    @Builder.Default @Min(0) private int cpu = CPU_MILLI_LIMIT_DEFAULT;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @TypeAlias("container_reserve")
  public static class Reserve {
    @Builder.Default @Min(0) private int memory = MEM_RESERVE_DEFAULT;
    @Builder.Default @Min(0) private int cpu = CPU_MILLI_RESERVE_DEFAULT;
  }
}
