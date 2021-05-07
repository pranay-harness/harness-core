package io.harness.pcf;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.pcf.model.PcfConstants.AUTOSCALING_APPS_PLUGIN_NAME;
import static io.harness.pcf.model.PcfConstants.CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfConstants.SYS_VAR_CF_PLUGIN_HOME;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.cfcli.CfCliCommandResolver;
import io.harness.pcf.model.CfCliVersion;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

@OwnedBy(HarnessTeam.CDP)
public class PcfUtils {
  public static final String BIN_BASH = "/bin/bash";

  private PcfUtils() {}

  public static boolean checkIfAppAutoscalarInstalled(final String cfCliPath, CfCliVersion cfCliVersion)
      throws PivotalClientApiException {
    boolean appAutoscalarInstalled;
    Map<String, String> map = new HashMap<>();
    map.put(CF_PLUGIN_HOME, resolvePcfPluginHome());
    String command =
        CfCliCommandResolver.getCheckingPluginsCliCommand(cfCliPath, cfCliVersion, AUTOSCALING_APPS_PLUGIN_NAME);
    ProcessExecutor processExecutor = createExecutorForAutoscalarPluginCheck(command, map);

    try {
      ProcessResult processResult = processExecutor.execute();
      appAutoscalarInstalled = isNotEmpty(processResult.outputUTF8());
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar plugin failed", e);
    }

    return appAutoscalarInstalled;
  }

  public static String resolvePcfPluginHome() {
    // look into java system variable
    final String sysVarPluginHome = System.getProperty(SYS_VAR_CF_PLUGIN_HOME);
    if (isNotEmpty(sysVarPluginHome)) {
      return sysVarPluginHome.trim();
    }
    // env variable
    final String envVarPluginHome = System.getenv(CF_PLUGIN_HOME);
    if (isNotEmpty(envVarPluginHome)) {
      return envVarPluginHome.trim();
    }
    // default is user home
    return System.getProperty("user.home");
  }

  public static ProcessExecutor createExecutorForAutoscalarPluginCheck(final String command, Map<String, String> map) {
    return new ProcessExecutor()
        .timeout(1, TimeUnit.MINUTES)
        .command(BIN_BASH, "-c", command)
        .readOutput(true)
        .environment(map);
  }
}
