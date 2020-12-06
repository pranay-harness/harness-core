package software.wings.beans;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DelegateTaskBroadcast {
  private String version;
  private String accountId;
  private String taskId;
  private boolean async;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;
}
