package software.wings.waitnotify;

import lombok.Data;
import software.wings.sm.DelegateMetaInfo;

@Data
public abstract class DelegateTaskNotifyResponseData implements NotifyResponseData {
  private DelegateMetaInfo delegateMetaInfo;
}
