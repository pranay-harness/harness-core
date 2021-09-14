/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.instana;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class InstanaAnalyzeMetrics {
  private List<Item> items;
  @Data
  @Builder
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Item {
    private Map<String, List<List<Number>>> metrics;
    private String name;
    private long timestamp;
  }
}
