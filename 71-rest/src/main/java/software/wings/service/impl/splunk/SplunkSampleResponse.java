package software.wings.service.impl.splunk;

import lombok.Builder;
import lombok.Value;

import java.util.List;
import java.util.Map;

@Value
@Builder
public class SplunkSampleResponse {
  List<String> rawSampleLogs;
  Map<String, String> sample;
}
