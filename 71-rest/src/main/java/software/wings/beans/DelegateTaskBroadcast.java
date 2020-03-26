package software.wings.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Set;

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
