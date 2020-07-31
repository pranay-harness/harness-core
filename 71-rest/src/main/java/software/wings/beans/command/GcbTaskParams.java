package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GcpConfig;
import software.wings.beans.GitConfig;
import software.wings.sm.states.gcbconfigs.GcbOptions;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
public class GcbTaskParams implements ExecutionCapabilityDemander {
  public enum GcbTaskType { START, POLL, CANCEL }

  @Nullable private String appId;
  @Nullable private String unitName;
  @Nullable private GcbTaskType type;
  @Nullable private String activityId;
  @Nullable private GcpConfig gcpConfig;
  @Nullable private List<EncryptedDataDetail> encryptedDataDetails;
  @Nullable private GcbOptions gcbOptions;
  @Nullable private String gcpConfigId;
  @Nullable private String accountId;

  @Nullable private GitConfig gitConfig;
  @Nullable private String buildId;
  @Nullable private String buildName;
  @Nullable private Map<String, String> substitutions;
  private long timeout;
  private long startTs;
  private boolean injectEnvVars;
  @Builder.Default private int pollFrequency = 5;

  @NotNull
  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return gcpConfig.fetchRequiredExecutionCapabilities();
  }
}
