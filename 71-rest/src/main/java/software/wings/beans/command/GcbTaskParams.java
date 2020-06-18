package software.wings.beans.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GcpConfig;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@Builder
public class GcbTaskParams implements ExecutionCapabilityDemander {
  public enum GcbTaskType { START, POLL }

  @NotNull private String appId;
  @NotNull private String unitName;
  @NotNull private String buildUrl;
  @NotNull private GcbTaskType type;
  @NotNull private String projectId;
  @NotNull private String activityId;
  @NotNull private GcpConfig gcpConfig;
  @NotNull private List<EncryptedDataDetail> encryptedDataDetails;

  @Nullable private String buildId;
  @Nullable private String buildName;
  @Nullable private String triggerId; // rework this into a list of options
  @Nullable private String branchName; // rework this into a list of options
  @Nullable private Map<String, String> parameters;
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
