/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.pcf;

import static io.harness.pcf.model.PcfConstants.BIN_BASH;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.SPACE;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.configuration.DelegateConfiguration;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.ProcessExecutionException;
import io.harness.pcf.cfcli.command.VersionCliCommand;
import io.harness.pcf.cfcli.command.VersionCliCommand.VersionOptions;
import io.harness.pcf.model.CfCliVersion;

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
@OwnedBy(HarnessTeam.CDP)
public class CfCliDelegateResolver {
  private static final int DEFAULT_CF_VERSION_CHECKING_TIMEOUT_IN_MIN = 1;
  private static final String DEFAULT_CF_CLI_INSTALLATION_PATH = "cf";

  @Inject private DelegateConfiguration delegateConfiguration;

  public Optional<String> getAvailableCfCliPathOnDelegate(CfCliVersion cliVersion) {
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

  public boolean isDelegateEligibleToExecuteCfCliCommand(CfCliVersion cliVersion) {
    if (cliVersion == null) {
      throw new InvalidArgumentsException("Parameter cliVersion cannot be null");
    }

    return CfCliVersion.V6 == cliVersion ? isDelegateEligibleToExecuteCfCliV6Command()
                                         : CfCliVersion.V7 == cliVersion && isDelegateEligibleToExecuteCfCliV7Command();
  }

  private boolean isDelegateEligibleToExecuteCfCliV6Command() {
    boolean cliV6Installed = verifyCliVersionInstalledOnDelegate(CfCliVersion.V6, DEFAULT_CF_CLI_INSTALLATION_PATH);
    if (cliV6Installed) {
      return true;
    }

    return isCustomBinaryInstalledOnDelegate(CfCliVersion.V6);
  }

  private boolean isDelegateEligibleToExecuteCfCliV7Command() {
    boolean cliV7Installed = verifyCliVersionInstalledOnDelegate(CfCliVersion.V7, DEFAULT_CF_CLI_INSTALLATION_PATH);
    if (cliV7Installed) {
      return true;
    }

    return isCustomBinaryInstalledOnDelegate(CfCliVersion.V7);
  }

  private boolean isCustomBinaryInstalledOnDelegate(CfCliVersion version) {
    String binaryPath = getCustomBinaryPathOnDelegateByVersion(version);
    if (StringUtils.isBlank(binaryPath)) {
      return false;
    }

    return verifyCliVersionInstalledOnDelegate(version, binaryPath);
  }

  private boolean verifyCliVersionInstalledOnDelegate(CfCliVersion version, final String cliPath) {
    String command = buildCliVersionCommand(version, cliPath);

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
      throw new ProcessExecutionException(format("Unable to execute bash command: %s", cmd), ex);
    }
  }

  private String processOutput(ProcessOutput output) {
    return output != null ? output.getUTF8() : null;
  }

  private static CfCliVersion extractCliVersion(final String processOutput) {
    if (StringUtils.isBlank(processOutput)) {
      return null;
    }

    return CfCliVersion.fromString(processOutput.trim().toLowerCase().split(SPACE)[2]);
  }

  private String getCustomBinaryPathOnDelegateByVersion(CfCliVersion version) {
    if (CfCliVersion.V6 == version) {
      return delegateConfiguration.getCfCli6Path();
    } else if (CfCliVersion.V7 == version) {
      return delegateConfiguration.getCfCli7Path();
    }
    return null;
  }

  private String buildCliVersionCommand(CfCliVersion version, final String cliPath) {
    return VersionCliCommand.builder()
        .cliPath(cliPath)
        .cliVersion(version)
        .options(VersionOptions.builder().version(true).build())
        .build()
        .getCommand();
  }
}
