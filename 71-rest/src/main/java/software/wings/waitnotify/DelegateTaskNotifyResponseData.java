package software.wings.waitnotify;

import io.harness.task.protocol.ResponseData;
import lombok.Data;
import software.wings.sm.DelegateMetaInfo;

@Data
public abstract class DelegateTaskNotifyResponseData implements ResponseData {
  private DelegateMetaInfo delegateMetaInfo;
}
