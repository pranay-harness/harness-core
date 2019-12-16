package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LogAnalysisResult {
  int label;
  String tag;
  String text;
}
