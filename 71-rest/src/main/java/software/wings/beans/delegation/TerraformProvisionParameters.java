package software.wings.beans.delegation;

import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Builder
@Value
public class TerraformProvisionParameters {
  public static final String CommandUnit = "Apply";

  private String accountId;
  private final String appId;
  private final String activityId;

  private final String entityId;

  private final String currentStateFileId;

  private final GitConfig sourceRepo;
  List<EncryptedDataDetail> sourceRepoEncryptionDetails;
  private final String scriptPath;
  private final Map<String, String> variables;
  private final Map<String, EncryptedDataDetail> encryptedVariables;

  private final String commandUnitName;
  private final long timeoutInMillis = TimeUnit.MINUTES.toMillis(10);
}
