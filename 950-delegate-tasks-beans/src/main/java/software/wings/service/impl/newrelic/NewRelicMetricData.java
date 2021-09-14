/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
