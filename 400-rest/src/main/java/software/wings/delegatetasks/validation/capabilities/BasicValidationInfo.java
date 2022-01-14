package software.wings.delegatetasks.validation.capabilities;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(HarnessTeam.DEL)
public class BasicValidationInfo {
  @NotNull private String accountId;
  @NotNull private String appId;
  @NotNull private String activityId;
  @NotNull private boolean executeOnDelegate;
  @NotNull private String publicDns;
}
