/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.service.impl.newrelic;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by rsingh on 8/30/17.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@Builder
public class NewRelicMetricData {
  private String from;
  private String to;

  private Set<String> metrics_not_found;
  private Set<String> metrics_found;

  private Set<NewRelicMetricSlice> metrics;

  @Data
  @EqualsAndHashCode(exclude = "timeslices")
  public static class NewRelicMetricSlice {
    private String name;
    private List<NewRelicMetricTimeSlice> timeslices;
  }

  @Data
  public static class NewRelicMetricTimeSlice {
    private String from;
    private String to;

    private Object values;
  }
}
