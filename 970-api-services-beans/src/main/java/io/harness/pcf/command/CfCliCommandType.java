package io.harness.pcf.command;

public enum CfCliCommandType implements CfCliCommandTemplate {
  VERSION {
    @Override
    public String getCommandTemplate() {
      return "${CLI_PATH} --version";
    }
  },
  LOGIN {
    @Override
    public String getCommandTemplate() {
      return "${CLI_PATH} login ${OPTIONS}";
    }
  };
}
