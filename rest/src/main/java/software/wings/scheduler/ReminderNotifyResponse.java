package software.wings.scheduler;

import lombok.Builder;
import lombok.Data;
import software.wings.waitnotify.NotifyResponseData;

import java.util.Map;

@Data
@Builder
public class ReminderNotifyResponse implements NotifyResponseData {
  Map<String, String> parameters;
}
