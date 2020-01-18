package io.harness.k8s.kubectl;

import static io.harness.k8s.kubectl.Utils.encloseWithQuotesIfNeeded;

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

  public VersionCommand version() {
    return new VersionCommand(this);
  }

  public ApplyCommand apply() {
    return new ApplyCommand(this);
  }

  public DeleteCommand delete() {
    return new DeleteCommand(this);
  }

  public GetCommand get() {
    return new GetCommand(this);
  }

  public AuthCommand auth() {
    return new AuthCommand(this);
  }

  public DescribeCommand describe() {
    return new DescribeCommand(this);
  }

  public RolloutCommand rollout() {
    return new RolloutCommand(this);
  }

  public GetPodCommand getPod() {
    return new GetPodCommand(new GetCommand(this));
  }

  public ScaleCommand scale() {
    return new ScaleCommand(this);
  }

  public String command() {
    StringBuilder command = new StringBuilder(128);
    if (StringUtils.isNotBlank(kubectlPath)) {
      command.append(encloseWithQuotesIfNeeded(kubectlPath)).append(' ');
    } else {
      command.append("kubectl ");
    }

    if (StringUtils.isNotBlank(configPath)) {
      command.append("--kubeconfig=" + encloseWithQuotesIfNeeded(configPath) + " ");
    }

    return command.toString();
  }

  public static String option(Option type, String value) {
    return "--" + type.toString() + "=" + value + " ";
  }

  public static String option(Option type, int value) {
    return "--" + type.toString() + "=" + value + " ";
  }

  public static String flag(Flag type) {
    return "--" + type.toString() + " ";
  }

  public static String flag(Flag type, boolean value) {
    return "--" + type.toString() + "=" + value + " ";
  }
}
