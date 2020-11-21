package software.wings.delegatetasks.validation.capabilities;

import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BasicValidationInfo {
  @NotNull private String accountId;
  @NotNull private String appId;
  @NotNull private String activityId;
  @NotNull private boolean executeOnDelegate;
  @NotNull private String publicDns;
}
