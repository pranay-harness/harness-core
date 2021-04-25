package io.harness.pcf.command;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.exception.InvalidArgumentsException;

public final class PcfCliCommandTemplateFactory {
  private static final String CLI_PATH_PLACEHOLDER = "${CLI_PATH}";

  private PcfCliCommandTemplateFactory() {}

  static String getPcfCliCommandTemplate(PcfCliCommandType commandType, final String cliPath) {
    if (isBlank(cliPath)) {
      throw new InvalidArgumentsException("Parameter cliPath is empty or null");
    }

    return commandType.getCommandTemplate().replace(CLI_PATH_PLACEHOLDER, cliPath);
  }
}
