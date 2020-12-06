package software.wings.service.impl.splunk;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class LogAnalysisResult {
  int label;
  String tag;
  String text;
}
