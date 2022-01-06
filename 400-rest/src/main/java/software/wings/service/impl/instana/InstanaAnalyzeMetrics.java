/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
