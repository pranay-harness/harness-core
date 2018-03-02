package software.wings.beans.delegation;

import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.List;

@Builder
@Getter
public class ShellScriptParameters {
  public static final String CommandUnit = "Execute";

  @Setter private String accountId;
  private final String appId;
  private final String activityId;

  private final String host;
  private final String userName;
  private final List<EncryptedDataDetail> keyEncryptedDataDetails;

  private final String script;

  public SshSessionConfig sshSessionConfig(EncryptionService encryptionService) throws IOException {
    return aSshSessionConfig()
        .withAccountId(accountId)
        .withAppId(appId)
        .withExecutionId(activityId)
        .withHost(host)
        .withUserName(userName)
        .withKey(encryptionService.getDecryptedValue(keyEncryptedDataDetails.get(0)))
        .withCommandUnitName(CommandUnit)
        .withPort(22)
        .build();
  }
}
