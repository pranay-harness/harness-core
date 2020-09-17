package io.harness.cvng.dashboard.beans;

import io.harness.cvng.analysis.entities.LogAnalysisResult.LogAnalysisTag;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class LogDataByTag {
  private Long timestamp;
  private List<CountByTag> countByTags;

  public void addCountByTag(CountByTag countByTag) {
    if (countByTags == null) {
      countByTags = new ArrayList<>();
    }
    countByTags.add(countByTag);
  }

  @Data
  @Builder
  public static class CountByTag {
    LogAnalysisTag tag;
    int count;
  }
}
