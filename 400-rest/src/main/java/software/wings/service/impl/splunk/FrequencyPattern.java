/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.splunk;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

// TODO Compress Frequency pattern in log_analysis_record.proto
@Data
@Builder
@AllArgsConstructor
public class FrequencyPattern {
  int label;
  List<Pattern> patterns;
  String text;

  @Data
  @Builder
  public static class Pattern {
    private List<Integer> sequence;
    private List<Long> timestamps;
  }
}
