package io.harness.pcf;

import static io.harness.pcf.model.PcfConstants.BIN_BASH;

import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.ProcessExecutionException;
import io.harness.pcf.command.CommandArguments;
import io.harness.pcf.command.PcfCliCommandTemplateResolver;
import io.harness.pcf.model.PcfCliVersion;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessOutput;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Singleton
@Slf4j
public class PcfCliDelegateResolver {
  private static final int DEFAULT_CF_VERSION_CHECKING_TIMEOUT_IN_MIN = 1;
  private static final String DEFAULT_CF_CLI_INSTALLATION_PATH = "cf";

  @Inject private DelegateConfiguration delegateConfiguration;

  public Optional<String> getAvailableCliPathOnDelegate(PcfCliVersion cliVersion) {
    if (cliVersion == null) {
      throw new InvalidArgumentsException("Parameter cliVersion cannot be null");
    }

    boolean cliInstalledOnDelegate = verifyCliVersionInstalledOnDelegate(cliVersion, DEFAULT_CF_CLI_INSTALLATION_PATH);
    if (cliInstalledOnDelegate) {
      return Optional.of(DEFAULT_CF_CLI_INSTALLATION_PATH);
    }

    boolean customBinaryInstalledOnDelegate = isCustomBinaryInstalledOnDelegate(cliVersion);
    return customBinaryInstalledOnDelegate ? Optional.ofNullable(getCustomBinaryPathOnDelegateByVersion(cliVersion))
                                           : Optional.empty();
  }

  public boolean isDelegateEligibleToExecuteCliCommand(PcfCliVersion cliVersion) {
    if (cliVersion == null) {
      throw new InvalidArgumentsException("Parameter cliVersion cannot be null");
    }

    return PcfCliVersion.V6 == cliVersion ? isDelegateEligibleToExecuteCliV6Command()
                                          : PcfCliVersion.V7 == cliVersion && isDelegateEligibleToExecuteCliV7Command();
  }

  private boolean isDelegateEligibleToExecuteCliV6Command() {
    boolean cliV6Installed = verifyCliVersionInstalledOnDelegate(PcfCliVersion.V6, DEFAULT_CF_CLI_INSTALLATION_PATH);
    if (cliV6Installed) {
      return true;
    }

    return isCustomBinaryInstalledOnDelegate(PcfCliVersion.V6);
  }

  private boolean isDelegateEligibleToExecuteCliV7Command() {
    boolean cliV7Installed = verifyCliVersionInstalledOnDelegate(PcfCliVersion.V7, DEFAULT_CF_CLI_INSTALLATION_PATH);
    if (cliV7Installed) {
      return true;
    }

    return isCustomBinaryInstalledOnDelegate(PcfCliVersion.V7);
  }

  private boolean isCustomBinaryInstalledOnDelegate(PcfCliVersion version) {
    String binaryPath = getCustomBinaryPathOnDelegateByVersion(version);
    if (StringUtils.isBlank(binaryPath)) {
      return false;
    }

    return verifyCliVersionInstalledOnDelegate(version, binaryPath);
  }

  private boolean verifyCliVersionInstalledOnDelegate(PcfCliVersion version, final String cliPath) {
    String command = PcfCliCommandTemplateResolver.getCliVersionCommand(buildCommandArguments(version, cliPath));

    ProcessResult processResult = executeCommand(command);

    return processResult.getExitValue() == 0 && version == extractCliVersion(processOutput(processResult.getOutput()));
  }

  @VisibleForTesting
  ProcessResult executeCommand(final String cmd) {
    try {
      return new ProcessExecutor()
          .timeout(DEFAULT_CF_VERSION_CHECKING_TIMEOUT_IN_MIN, TimeUnit.MINUTES)
          .command(BIN_BASH, "-c", cmd)
          .readOutput(true)
          .redirectOutput(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              log.info(line);
            }
          })
          .redirectError(new LogOutputStream() {
            @Override
            protected void processLine(String line) {
              log.error(line);
            }
          })
          .execute();
    } catch (Exception ex) {
      throw new ProcessExecutionException("Unable to execute bash command", ex);
    }
  }

  private String processOutput(ProcessOutput output) {
    return output != null ? output.getUTF8() : null;
  }

  private static PcfCliVersion extractCliVersion(final String processOutput) {
    if (StringUtils.isBlank(processOutput)) {
      return null;
    }

    return PcfCliVersion.fromString(processOutput.trim().toLowerCase().split(SPACE)[2]);
  }

  private String getCustomBinaryPathOnDelegateByVersion(PcfCliVersion version) {
    if (PcfCliVersion.V6 == version) {
      return delegateConfiguration.getCfCli6Path();
    } else if (PcfCliVersion.V7 == version) {
      return delegateConfiguration.getCfCli7Path();
    }
    return null;
  }

  private CommandArguments buildCommandArguments(PcfCliVersion version, final String cliPath) {
    return CommandArguments.builder().cliPath(cliPath).cliVersion(version).build();
  }
}
