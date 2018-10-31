package software.wings.beans.delegation;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Value
@Builder
public class TerraformProvisionParameters {
  private static final long TIMEOUT_IN_MINUTES = 30;

  public enum TerraformCommand { APPLY, DESTROY }

  public enum TerraformCommandUnit {
    Apply,
    Adjust,
    Destroy,
    Rollback;
  }

  private String accountId;
  private final String activityId;
  private final String appId;
  private final String entityId;
  private final String currentStateFileId;
  private final GitConfig sourceRepo;
  List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private final String scriptPath;
  private final Map<String, String> variables;
  private final Map<String, EncryptedDataDetail> encryptedVariables;

  private final Map<String, String> backendConfigs;
  private final Map<String, EncryptedDataDetail> encryptedBackendConfigs;

  private final TerraformCommand command;
  private final TerraformCommandUnit commandUnit;
  private final long timeoutInMillis = TimeUnit.MINUTES.toMillis(TIMEOUT_IN_MINUTES);
}
