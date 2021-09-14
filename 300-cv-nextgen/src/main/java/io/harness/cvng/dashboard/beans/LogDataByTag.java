/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.dashboard.beans;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

@Data
@Builder
public class LogDataByTag implements Comparable<LogDataByTag> {
  private Long timestamp;
  private List<CountByTag> countByTags;

  public void addCountByTag(CountByTag countByTag) {
    if (countByTags == null) {
      countByTags = new ArrayList<>();
    }
    countByTags.add(countByTag);
  }

  @Override
  public int compareTo(@NotNull LogDataByTag o) {
    return this.timestamp.compareTo(o.timestamp);
  }

  @Data
  @Builder
  public static class CountByTag {
    LogAnalysisTag tag;
    int count;
  }
}
