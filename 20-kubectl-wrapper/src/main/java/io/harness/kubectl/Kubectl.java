package io.harness.kubectl;

import org.apache.commons.lang3.StringUtils;

public class Kubectl {
  private final String kubectlPath;
  private final String configPath;

  private Kubectl(String kubectlPath, String configPath) {
    this.kubectlPath = kubectlPath;
    this.configPath = configPath;
  }

  public static Kubectl client(String kubectlPath, String configPath) {
    return new Kubectl(kubectlPath, configPath);
  }

  VersionCommand version() {
    return new VersionCommand(this);
  }

  ApplyCommand apply() {
    return new ApplyCommand(this);
  }

  GetCommand get() {
    return new GetCommand(this);
  }

  public String command() {
    StringBuilder command = new StringBuilder(128);
    if (StringUtils.isNotBlank(kubectlPath)) {
      command.append(kubectlPath);
    } else {
      command.append("kubectl ");
    }

    if (StringUtils.isNotBlank(configPath)) {
      command.append("--kubeconfig='" + configPath + "' ");
    }

    return command.toString();
  }

  public static String option(Option type, String value) {
    return "--" + type.toString() + "=" + value + " ";
  }

  public static String flag(Flag type) {
    return "--" + type.toString() + " ";
  }
}
