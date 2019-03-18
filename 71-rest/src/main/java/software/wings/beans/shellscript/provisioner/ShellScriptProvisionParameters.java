package software.wings.beans.shellscript.provisioner;

import io.harness.delegate.task.TaskParameters;
import lombok.Builder;
import lombok.Value;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.Map;

@Value
@Builder
public class ShellScriptProvisionParameters implements TaskParameters {
  private String scriptBody;
  private long timeoutInMillis;
  private Map<String, String> textVariables;
  private Map<String, EncryptedDataDetail> encryptedVariables;
  private String entityId;

  private String accountId;
  private String appId;
  private String activityId;
  private String commandUnit;
}
