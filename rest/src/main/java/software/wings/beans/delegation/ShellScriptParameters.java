package software.wings.beans.delegation;

import static software.wings.core.ssh.executors.SshSessionConfig.Builder.aSshSessionConfig;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import software.wings.api.ScriptType;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.core.local.executors.ShellExecutorConfig;
import software.wings.core.ssh.executors.SshSessionConfig;
import software.wings.core.winrm.executors.WinRmSessionConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.ShellScriptState;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Builder
@Getter
public class ShellScriptParameters {
  public static final String CommandUnit = "Execute";

  @Setter private String accountId;
  private final String appId;
  private final String activityId;
  private final String host;
  private final String userName;
  private final ShellScriptState.ConnectionType connectionType;
  private final List<EncryptedDataDetail> keyEncryptedDataDetails;
  private final WinRmConnectionAttributes winrmConnectionAttributes;
  private final List<EncryptedDataDetail> winrmConnectionEncryptedDataDetails;

  private final Map<String, String> environment;
  private final String workingDirectory;
  private final ScriptType scriptType;
  private final String script;
  private final boolean executeOnDelegate;

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

  public WinRmSessionConfig winrmSessionConfig(EncryptionService encryptionService) throws IOException {
    encryptionService.decrypt(winrmConnectionAttributes, winrmConnectionEncryptedDataDetails);
    return WinRmSessionConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(CommandUnit)
        .hostname(host)
        .authenticationScheme(winrmConnectionAttributes.getAuthenticationScheme())
        .domain(winrmConnectionAttributes.getDomain())
        .username(winrmConnectionAttributes.getUsername())
        .password(String.valueOf(winrmConnectionAttributes.getPassword()))
        .port(winrmConnectionAttributes.getPort())
        .useSSL(winrmConnectionAttributes.isUseSSL())
        .skipCertChecks(winrmConnectionAttributes.isSkipCertChecks())
        .workingDirectory(workingDirectory)
        .environment(environment == null ? Collections.emptyMap() : environment)
        .build();
  }

  public ShellExecutorConfig processExecutorConfig() {
    return ShellExecutorConfig.builder()
        .accountId(accountId)
        .appId(appId)
        .executionId(activityId)
        .commandUnitName(CommandUnit)
        .workingDirectory(workingDirectory)
        .environment(environment == null ? Collections.emptyMap() : environment)
        .build();
  }
}
