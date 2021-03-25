package io.harness.pcf;

import static io.harness.data.structure.HasPredicate.hasSome;
import static io.harness.pcf.model.PcfConstants.CF_COMMAND_FOR_CHECKING_AUTOSCALAR;
import static io.harness.pcf.model.PcfConstants.CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfConstants.SYS_VAR_CF_PLUGIN_HOME;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;

public class PcfUtils {
  public static final String BIN_BASH = "/bin/bash";

  public static boolean checkIfAppAutoscalarInstalled() throws PivotalClientApiException {
    boolean appAutoscalarInstalled;
    Map<String, String> map = new HashMap();
    map.put(CF_PLUGIN_HOME, resolvePcfPluginHome());
    ProcessExecutor processExecutor = createExecutorForAutoscalarPluginCheck(map);

    try {
      ProcessResult processResult = processExecutor.execute();
      appAutoscalarInstalled = hasSome(processResult.outputUTF8());
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar plugin failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar plugin failed", e);
    }

    return appAutoscalarInstalled;
  }

  public static String resolvePcfPluginHome() {
    // look into java system variable
    final String sysVarPluginHome = System.getProperty(SYS_VAR_CF_PLUGIN_HOME);
    if (hasSome(sysVarPluginHome)) {
      return sysVarPluginHome.trim();
    }
    // env variable
    final String envVarPluginHome = System.getenv(CF_PLUGIN_HOME);
    if (hasSome(envVarPluginHome)) {
      return envVarPluginHome.trim();
    }
    // default is user home
    return System.getProperty("user.home");
  }

  public static ProcessExecutor createExecutorForAutoscalarPluginCheck(Map<String, String> map) {
    return new ProcessExecutor()
        .timeout(1, TimeUnit.MINUTES)
        .command(BIN_BASH, "-c", CF_COMMAND_FOR_CHECKING_AUTOSCALAR)
        .readOutput(true)
        .environment(map);
  }
}
