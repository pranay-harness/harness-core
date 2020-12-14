package io.harness.delegate.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DelegateSelectionLogResponse {
  List<DelegateSelectionLogParams> delegateSelectionLogs;
  Map<String, String> taskSetupAbstractions;
}
