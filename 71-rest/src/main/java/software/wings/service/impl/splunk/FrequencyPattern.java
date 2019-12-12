package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Data;

import java.util.List;

// TODO Compress Frequency pattern in log_analysis_record.proto
// TODO Add Tag variable to Frequency Pattern.
@Data
@Builder
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
