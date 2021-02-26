package software.wings.beans;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(Module._420_DELEGATE_SERVICE)
public class DelegateTaskBroadcast {
  private String version;
  private String accountId;
  private String taskId;
  private boolean async;
  private String preAssignedDelegateId;
  private Set<String> alreadyTriedDelegates;
}
