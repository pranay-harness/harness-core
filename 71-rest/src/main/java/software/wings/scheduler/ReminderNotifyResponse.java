package software.wings.scheduler;

import io.harness.task.protocol.ResponseData;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ReminderNotifyResponse implements ResponseData {
  Map<String, String> parameters;
}
