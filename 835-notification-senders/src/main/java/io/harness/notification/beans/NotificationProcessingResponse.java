package io.harness.notification.beans;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class NotificationProcessingResponse {
  public static final NotificationProcessingResponse trivialResponseWithRetries =
      new NotificationProcessingResponse(Collections.emptyList(), true);
  public static final NotificationProcessingResponse trivialResponseWithNoRetries =
      new NotificationProcessingResponse(Collections.emptyList(), false);
  public static NotificationProcessingResponse allSent(int nrecipients) {
    return NotificationProcessingResponse.builder()
        .result(new ArrayList<>(Collections.nCopies(nrecipients, true)))
        .build();
  }
  public static NotificationProcessingResponse nonSent(int nrecipients) {
    return NotificationProcessingResponse.builder()
        .result(new ArrayList<>(Collections.nCopies(nrecipients, false)))
        .shouldRetry(true)
        .build();
  }

  public static boolean isNotificationResquestFailed(NotificationProcessingResponse response) {
    return !response.getResult().stream().reduce(false, (e1, e2) -> e1 || e2);
  }

  public static boolean isNotificationResquestFailed(List<Boolean> response) {
    return !response.stream().reduce(false, (e1, e2) -> e1 || e2);
  }

  List<Boolean> result;
  @Builder.Default boolean shouldRetry = false;
}
