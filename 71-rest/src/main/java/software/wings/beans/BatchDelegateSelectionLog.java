package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchDelegateSelectionLog {
  private List<DelegateSelectionLog> delegateSelectionLogs;

  public void append(DelegateSelectionLog delegateSelectionLog) {
    delegateSelectionLogs.add(delegateSelectionLog);
  }
}
