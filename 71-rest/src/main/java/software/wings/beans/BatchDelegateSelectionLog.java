package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchDelegateSelectionLog {
  private String taskId;

  @Builder.Default private List<DelegateSelectionLog> delegateSelectionLogs = new ArrayList<>();

  public void append(DelegateSelectionLog delegateSelectionLog) {
    Optional<DelegateSelectionLog> logs = delegateSelectionLogs.stream()
                                              .filter(log
                                                  -> log.getAccountId().equals(delegateSelectionLog.getAccountId())
                                                      && log.getTaskId().equals(delegateSelectionLog.getTaskId())
                                                      && log.getMessage().equals(delegateSelectionLog.getMessage()))
                                              .findFirst();
    if (logs.isPresent()) {
      logs.get().getDelegateIds().addAll(delegateSelectionLog.getDelegateIds());
    } else {
      delegateSelectionLogs.add(delegateSelectionLog);
    }
  }
}
