package io.harness.pcf.cfcli.client;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.LogLevel.ERROR;
import static io.harness.pcf.model.PcfConstants.CF_DOCKER_CREDENTIALS;
import static io.harness.pcf.model.PcfConstants.CF_HOME;
import static io.harness.pcf.model.PcfConstants.CF_PASSWORD;
import static io.harness.pcf.model.PcfConstants.CF_PLUGIN_HOME;
import static io.harness.pcf.model.PcfConstants.CF_USERNAME;
import static io.harness.pcf.model.PcfConstants.DISABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.ENABLE_AUTOSCALING;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PATH_SEPARATOR_CHAR;
import static io.harness.pcf.model.PcfConstants.PCF_ROUTE_PORT_SEPARATOR;
import static io.harness.pcf.model.PcfConstants.PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_HTTP;
import static io.harness.pcf.model.PcfRouteType.PCF_ROUTE_TYPE_TCP;

import static software.wings.beans.LogColor.Green;
import static software.wings.beans.LogColor.Red;
import static software.wings.beans.LogColor.White;
import static software.wings.beans.LogHelper.color;
import static software.wings.beans.LogWeight.Bold;

import static com.google.common.base.Charsets.UTF_8;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toSet;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.kubectl.Utils;
import io.harness.logging.LogCallback;
import io.harness.network.Http;
import io.harness.pcf.CfCliClient;
import io.harness.pcf.CfSdkClient;
import io.harness.pcf.PcfUtils;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.cfcli.CfCliCommandResolver;
import io.harness.pcf.cfcli.CfCliCommandType;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfManifestFileData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CfRunPluginScriptRequestData;
import io.harness.pcf.model.PcfRouteInfo;
import io.harness.pcf.model.PcfRouteInfo.PcfRouteInfoBuilder;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.cloudfoundry.operations.domains.Domain;
import org.jetbrains.annotations.NotNull;
import org.zeroturnaround.exec.ProcessExecutor;
import org.zeroturnaround.exec.ProcessResult;
import org.zeroturnaround.exec.StartedProcess;
import org.zeroturnaround.exec.stream.LogOutputStream;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class CfCliClientImpl implements CfCliClient {
  public static final String BIN_BASH = "/bin/bash";
  public static final String SUCCESS = "SUCCESS";
  public static final String PCF_PROXY_PROPERTY = "https_proxy";

  @Inject private CfSdkClient cfSdkClient;

  @Override
  public void pushAppByCli(CfCreateApplicationRequestData requestData, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    log.info("Using CLI to create application");

    // Create a new filePath.
    CfRequestConfig pcfRequestConfig = requestData.getCfRequestConfig();

    int exitCode = 1;
    try {
      String finalFilePath = requestData.getManifestFilePath().replace(".yml", "_1.yml");
      FileUtils.writeStringToFile(new File(finalFilePath), requestData.getFinalManifestYaml(), UTF_8);
      logManifestFile(finalFilePath, executionLogCallback);

      executionLogCallback.saveExecutionLog("# CF_HOME value: " + requestData.getConfigPathVar());
      boolean loginSuccessful = true;
      if (!requestData.getCfRequestConfig().isLoggedin()) {
        loginSuccessful = doLogin(pcfRequestConfig, executionLogCallback, requestData.getConfigPathVar());
      }

      if (loginSuccessful) {
        exitCode = doCfPush(pcfRequestConfig, executionLogCallback, finalFilePath, requestData);
      }
    } catch (Exception e) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred while creating Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: App creation process Failed :  ")
                                              .toString(),
          e);
    }

    if (exitCode != 0) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occured while creating Application: ")
                                              .append(pcfRequestConfig.getApplicationName())
                                              .append(", Error: App creation process ExitCode:  ")
                                              .append(exitCode)
                                              .toString());
    }
  }

  @Override
  public void performConfigureAutoscaler(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    int exitCode = 1;

    try {
      // First login
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);

      if (loginSuccessful) {
        logManifestFile(appAutoscalarRequestData.getAutoscalarFilePath(), executionLogCallback);

        // perform configure-autoscalar command
        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(appAutoscalarRequestData.getTimeoutInMins(),
            getConfigureAutosaclarCfCliCommand(appAutoscalarRequestData),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);
        exitCode = processExecutor.execute().getExitValue();
      }
    } catch (InterruptedException e) {
      execeptionForAutoscalarConfigureFailure(appAutoscalarRequestData.getApplicationName(), e);
    } catch (Exception e) {
      execeptionForAutoscalarConfigureFailure(appAutoscalarRequestData.getApplicationName(), e);
    }

    if (exitCode != 0) {
      throw new PivotalClientApiException(
          new StringBuilder()
              .append("Exception occurred while Configuring autoscalar for Application: ")
              .append(appAutoscalarRequestData.getApplicationName())
              .append(", Error: App Autoscalar configuration Failed :  ")
              .append(exitCode)
              .toString());
    }
  }

  @Override
  @VisibleForTesting
  public void changeAutoscalerState(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback, boolean enable) throws PivotalClientApiException {
    int exitCode = 1;

    try {
      // First login
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);

      if (loginSuccessful) {
        // perform enable/disable autoscalar
        String completeCommand = generateChangeAutoscalarStateCommand(appAutoscalarRequestData, enable);

        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(appAutoscalarRequestData.getTimeoutInMins(),
            completeCommand, getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);
        exitCode = processExecutor.execute().getExitValue();
      }
    } catch (InterruptedException e) {
      exceptionForAutoscalarStateChangeFailure(appAutoscalarRequestData.getApplicationName(), enable, e);
      return;
    } catch (Exception e) {
      exceptionForAutoscalarStateChangeFailure(appAutoscalarRequestData.getApplicationName(), enable, e);
    }
    if (exitCode != 0) {
      throw new PivotalClientApiException(new StringBuilder()
                                              .append("Exception occurred for Application: ")
                                              .append(appAutoscalarRequestData.getApplicationName())
                                              .append(", for action: ")
                                              .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                              .append(exitCode)
                                              .toString());
    }
  }

  @Override
  public boolean checkIfAppHasAutoscalerAttached(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    boolean appAutoscalarInstalled = false;
    executionLogCallback.saveExecutionLog("\n# Checking if Application: "
        + appAutoscalarRequestData.getApplicationName() + " has Autoscalar Bound to it");

    try {
      CfRequestConfig pcfRequestConfig = appAutoscalarRequestData.getCfRequestConfig();
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, executionLogCallback);
      if (loginSuccessful) {
        ProcessExecutor processExecutor = createProccessExecutorForPcfTask(1,
            CfCliCommandResolver.getAutoscalingAppsCliCommandWithGrep(pcfRequestConfig.getCfCliPath(),
                pcfRequestConfig.getCfCliVersion(), appAutoscalarRequestData.getApplicationGuid()),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), executionLogCallback);

        ProcessResult processResult = processExecutor.execute();
        appAutoscalarInstalled = isNotEmpty(processResult.outputUTF8());
      }
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar Binding failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar Binding failed", e);
    }

    return appAutoscalarInstalled;
  }

  @Override
  public boolean checkIfAppHasAutoscalerWithExpectedState(
      CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback) throws PivotalClientApiException {
    boolean appAutoscalarInExpectedState = false;
    CfRequestConfig pcfRequestConfig = appAutoscalarRequestData.getCfRequestConfig();
    logCallback.saveExecutionLog("\n# Checking if Application: " + appAutoscalarRequestData.getApplicationName()
        + " has Autoscalar Bound to it");

    try {
      boolean loginSuccessful = logInForAppAutoscalarCliCommand(appAutoscalarRequestData, logCallback);
      if (loginSuccessful) {
        ProcessExecutor executor = createProccessExecutorForPcfTask(1,
            CfCliCommandResolver.getAutoscalingAppsCliCommandWithGrep(pcfRequestConfig.getCfCliPath(),
                pcfRequestConfig.getCfCliVersion(), appAutoscalarRequestData.getApplicationGuid()),
            getAppAutoscalarEnvMapForCustomPlugin(appAutoscalarRequestData), logCallback);

        ProcessResult processResult = executor.execute();
        String output = processResult.outputUTF8();
        if (isEmpty(output)) {
          logCallback.saveExecutionLog("\n# No App Autoscalar Bound to App");
        } else {
          logCallback.saveExecutionLog("# App Autoscalar Current State: " + output);
          String status = appAutoscalarRequestData.isExpectedEnabled() ? " true " : " false ";
          if (output.contains(status)) {
            appAutoscalarInExpectedState = true;
          }
        }
      }
    } catch (InterruptedException e) {
      throw new PivotalClientApiException("check for App Autoscalar Binding failed", e);
    } catch (Exception e) {
      throw new PivotalClientApiException("check for AppAutoscalar Binding failed", e);
    }

    return appAutoscalarInExpectedState;
  }

  @VisibleForTesting
  String generateChangeAutoscalarStateCommand(CfAppAutoscalarRequestData appAutoscalarRequestData, boolean enable) {
    CfRequestConfig pcfRequestConfig = appAutoscalarRequestData.getCfRequestConfig();
    return enable ? CfCliCommandResolver.getEnableAutoscalingCliCommand(pcfRequestConfig.getCfCliPath(),
               pcfRequestConfig.getCfCliVersion(), appAutoscalarRequestData.getApplicationName())
                  : CfCliCommandResolver.getDisableAutoscalingCliCommand(pcfRequestConfig.getCfCliPath(),
                      pcfRequestConfig.getCfCliVersion(), appAutoscalarRequestData.getApplicationName());
  }

  @VisibleForTesting
  boolean logInForAppAutoscalarCliCommand(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws InterruptedException, TimeoutException, IOException {
    boolean loginSuccessful = true;
    if (!appAutoscalarRequestData.getCfRequestConfig().isLoggedin()) {
      loginSuccessful = doLogin(appAutoscalarRequestData.getCfRequestConfig(), executionLogCallback,
          appAutoscalarRequestData.getConfigPathVar());
    }
    appAutoscalarRequestData.getCfRequestConfig().setLoggedin(loginSuccessful);
    return loginSuccessful;
  }

  @VisibleForTesting
  Map<String, String> getAppAutoscalarEnvMapForCustomPlugin(CfAppAutoscalarRequestData appAutoscalarRequestData) {
    Map<String, String> environmentMapForPcfExecutor = getEnvironmentMapForPcfExecutor(
        appAutoscalarRequestData.getCfRequestConfig().getEndpointUrl(), appAutoscalarRequestData.getConfigPathVar());
    // set CUSTOM_PLUGIN_HOME, NEEDED FOR AUTO-SCALAR PLUIN
    environmentMapForPcfExecutor.put(CF_PLUGIN_HOME, PcfUtils.resolvePcfPluginHome());
    return environmentMapForPcfExecutor;
  }

  @VisibleForTesting
  ProcessExecutor createProccessExecutorForPcfTask(
      long timeout, String command, Map<String, String> env, LogCallback executionLogCallback) {
    return new ProcessExecutor()
        .timeout(timeout, TimeUnit.MINUTES)
        .command(BIN_BASH, "-c", command)
        .readOutput(true)
        .environment(env)
        .redirectOutput(new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line);
          }
        });
  }

  private void exceptionForAutoscalarStateChangeFailure(String appName, boolean enable, Exception e)
      throws PivotalClientApiException {
    throw new PivotalClientApiException(new StringBuilder()
                                            .append("Exception occurred for Application: ")
                                            .append(appName)
                                            .append(", for action: ")
                                            .append(enable ? ENABLE_AUTOSCALING : DISABLE_AUTOSCALING)
                                            .append(", Error: ")
                                            .append(e)
                                            .toString(),
        e);
  }

  private void execeptionForAutoscalarConfigureFailure(String applicationName, Exception e)
      throws PivotalClientApiException {
    throw new PivotalClientApiException(new StringBuilder(128)
                                            .append("Exception occurred while Configuring autoscalar for Application: ")
                                            .append(applicationName)
                                            .append(", Error: ")
                                            .append(e)
                                            .toString(),
        e);
  }

  @NotNull
  private String getConfigureAutosaclarCfCliCommand(CfAppAutoscalarRequestData appAutoscalarRequestData) {
    CfRequestConfig pcfRequestConfig = appAutoscalarRequestData.getCfRequestConfig();
    return CfCliCommandResolver.getConfigureAutoscalingCliCommand(pcfRequestConfig.getCfCliPath(),
        pcfRequestConfig.getCfCliVersion(), appAutoscalarRequestData.getApplicationName(),
        appAutoscalarRequestData.getAutoscalarFilePath());
  }

  private int doCfPush(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback, String finalFilePath,
      CfCreateApplicationRequestData requestData) throws InterruptedException, TimeoutException, IOException {
    executionLogCallback.saveExecutionLog("# Performing \"cf push\"");
    Map<String, String> environmentMapForPcfExecutor = getEnvironmentMapForPcfPush(requestData);
    String command = constructCfPushCommand(requestData, finalFilePath);
    ProcessExecutor processExecutor = new ProcessExecutor()
                                          .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
                                          .command(BIN_BASH, "-c", command)
                                          .readOutput(true)
                                          .environment(environmentMapForPcfExecutor)
                                          .redirectOutput(new LogOutputStream() {
                                            @Override
                                            protected void processLine(String line) {
                                              executionLogCallback.saveExecutionLog(line);
                                            }
                                          });
    ProcessResult processResult = processExecutor.execute();
    int result = processResult.getExitValue();
    if (result != 0) {
      executionLogCallback.saveExecutionLog(format(processResult.outputUTF8(), Bold, Red), ERROR);
    } else {
      executionLogCallback.saveExecutionLog(format(SUCCESS, Bold, Green));
    }
    return result;
  }

  @VisibleForTesting
  Map<String, String> getEnvironmentMapForPcfPush(CfCreateApplicationRequestData requestData) {
    Map<String, String> environmentMapForPcfExecutor = getEnvironmentMapForPcfExecutor(
        requestData.getCfRequestConfig().getEndpointUrl(), requestData.getConfigPathVar());
    if (requestData.isDockerBasedDeployment()) {
      char[] password = requestData.getPassword();
      if (!isEmpty(password)) {
        environmentMapForPcfExecutor.put(CF_DOCKER_CREDENTIALS, String.valueOf(password));
      }
    }
    return environmentMapForPcfExecutor;
  }

  private String constructCfPushCommand(CfCreateApplicationRequestData requestData, String finalFilePath) {
    CfRequestConfig pcfRequestConfig = requestData.getCfRequestConfig();
    if (!requestData.isVarsYmlFilePresent()) {
      return CfCliCommandResolver.getPushCliCommand(
          pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion(), finalFilePath);
    }

    List<String> varFiles = new ArrayList<>();
    CfManifestFileData pcfManifestFileData = requestData.getPcfManifestFileData();
    if (isNotEmpty(pcfManifestFileData.getVarFiles())) {
      pcfManifestFileData.getVarFiles().forEach(varsFile -> {
        if (varsFile != null) {
          varFiles.add(String.valueOf(varsFile.getAbsoluteFile()));
        }
      });
    }

    return CfCliCommandResolver.getPushCliCommand(
        pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion(), finalFilePath, varFiles);
  }

  @VisibleForTesting
  Map<String, String> getEnvironmentMapForPcfExecutor(String endpointUrl, String configPathVar) {
    return getEnvironmentMapForPcfExecutor(endpointUrl, configPathVar, null);
  }

  private Map<String, String> getEnvironmentMapForPcfExecutor(
      String endpointUrl, String configPathVar, String pluginHomeAbsPath) {
    final Map<String, String> map = new HashMap<>();
    map.put(CF_HOME, configPathVar);
    if (isNotEmpty(pluginHomeAbsPath)) {
      map.put(CF_PLUGIN_HOME, pluginHomeAbsPath);
    }
    addProxyPropertyIfRequired(endpointUrl, map);
    return map;
  }

  private void addProxyPropertyIfRequired(String endpointUrl, Map<String, String> map) {
    String proxyHostName = Http.getProxyHostName();
    if (!Http.shouldUseNonProxy(endpointUrl) && isNotEmpty(proxyHostName)) {
      String authDetails = "";
      if (Http.getProxyPassword() != null && Http.getProxyUserName() != null) {
        authDetails = String.format("%s:%s@", Http.getProxyUserName(), Http.getProxyPassword());
      }
      String portProperty = Http.getProxyPort();
      String portDetails = "";
      if (!portProperty.equals("80")) {
        portDetails = String.format(":%s", Http.getProxyPort());
      }
      map.put(PCF_PROXY_PROPERTY, Http.getProxyScheme() + "://" + authDetails + proxyHostName + portDetails);
    }
  }

  int executeCommand(String command, Map<String, String> env, LogCallback logCallback)
      throws IOException, InterruptedException, TimeoutException {
    logCallback.saveExecutionLog(format("Executing command: [%s]", command));
    ProcessExecutor executor = new ProcessExecutor()
                                   .timeout(5, TimeUnit.MINUTES)
                                   .command(BIN_BASH, "-c", command)
                                   .readOutput(true)
                                   .environment(env)
                                   .redirectOutput(new LogOutputStream() {
                                     @Override
                                     protected void processLine(String line) {
                                       logCallback.saveExecutionLog(line);
                                     }
                                   });
    ProcessResult result = executor.execute();
    int resultCode = result.getExitValue();
    if (resultCode != 0) {
      logCallback.saveExecutionLog(format(result.outputUTF8(), Bold, Red), ERROR);
    } else {
      logCallback.saveExecutionLog(format(SUCCESS, Bold, Green));
    }
    return resultCode;
  }

  boolean doLogin(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback, String configPathVar)
      throws IOException, InterruptedException, TimeoutException {
    executionLogCallback.saveExecutionLog("# Performing \"login\"");

    String command;
    int exitValue;
    Map<String, String> env = getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(), configPathVar);

    command = CfCliCommandResolver.getApiCommand(
        pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getEndpointUrl(), true);
    exitValue = executeCommand(command, env, executionLogCallback);

    if (exitValue == 0) {
      Map<String, String> envForAuth = new HashMap<>(env);
      envForAuth.put(CF_USERNAME, pcfRequestConfig.getUserName());
      envForAuth.put(CF_PASSWORD, pcfRequestConfig.getPassword());
      command =
          CfCliCommandResolver.getAuthCommand(pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion());
      exitValue = executeCommand(command, envForAuth, executionLogCallback);
    }

    if (exitValue == 0) {
      command = CfCliCommandResolver.getTargetCommand(pcfRequestConfig.getCfCliPath(),
          pcfRequestConfig.getCfCliVersion(), Utils.encloseWithQuotesIfNeeded(pcfRequestConfig.getOrgName()),
          Utils.encloseWithQuotesIfNeeded(pcfRequestConfig.getSpaceName()));
      exitValue = executeCommand(command, env, executionLogCallback);
    }

    executionLogCallback.saveExecutionLog(exitValue == 0 ? "# Login Successful" : "# Login Failed");
    return exitValue == 0;
  }

  private void logManifestFile(String finalFilePath, LogCallback executionLogCallback) {
    String content;
    try {
      content = new String(Files.readAllBytes(Paths.get(finalFilePath)), Charsets.UTF_8);
      executionLogCallback.saveExecutionLog(
          new StringBuilder(128).append("# Manifest File Content: \n").append(content).append('\n').toString());
      log.info(new StringBuilder(128)
                   .append("Manifest File at Path: ")
                   .append(finalFilePath)
                   .append(", contents are \n")
                   .append(content)
                   .toString());
    } catch (Exception e) {
      log.warn("Failed to log manifest file contents at path : " + finalFilePath);
    }
  }

  @Override
  public void runPcfPluginScript(CfRunPluginScriptRequestData pcfRunPluginScriptRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException {
    CfRequestConfig pcfRequestConfig = pcfRunPluginScriptRequestData.getCfRequestConfig();
    int exitCode = -1;
    try {
      executionLogCallback.saveExecutionLog("# Final Script to execute :");
      executionLogCallback.saveExecutionLog("# ------------------------------------------ \n");
      executionLogCallback.saveExecutionLog(pcfRunPluginScriptRequestData.getFinalScriptString());
      executionLogCallback.saveExecutionLog("\n# ------------------------------------------ ");
      executionLogCallback.saveExecutionLog(
          "\n# CF_HOME value: " + pcfRunPluginScriptRequestData.getWorkingDirectory());
      final String pcfPluginHome = PcfUtils.resolvePcfPluginHome();
      executionLogCallback.saveExecutionLog("# CF_PLUGIN_HOME value: " + pcfPluginHome);
      boolean loginSuccessful =
          doLogin(pcfRequestConfig, executionLogCallback, pcfRunPluginScriptRequestData.getWorkingDirectory());
      if (loginSuccessful) {
        executionLogCallback.saveExecutionLog("# Executing pcf plugin script :");

        ProcessExecutor processExecutor =
            new ProcessExecutor()
                .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
                .command(BIN_BASH, "-c", pcfRunPluginScriptRequestData.getFinalScriptString())
                .readOutput(true)
                .environment(getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(),
                    pcfRunPluginScriptRequestData.getWorkingDirectory(), pcfPluginHome))
                .redirectOutput(new LogOutputStream() {
                  @Override
                  protected void processLine(String line) {
                    executionLogCallback.saveExecutionLog(line);
                  }
                });
        ProcessResult processResult = runProcessExecutor(processExecutor);
        exitCode = processResult.getExitValue();
        if (exitCode == 0) {
          executionLogCallback.saveExecutionLog(format(SUCCESS, Bold, Green));
        } else {
          executionLogCallback.saveExecutionLog(format(processResult.outputUTF8(), Bold, Red), ERROR);
        }
      }
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception occurred while running pcf plugin script", e);
    }
    if (exitCode != 0) {
      throw new PivotalClientApiException("Exception occurred while running pcf plugin script"
          + ", Error: Plugin Script process ExitCode:  " + exitCode);
    }
  }

  @VisibleForTesting
  ProcessResult runProcessExecutor(ProcessExecutor processExecutor)
      throws InterruptedException, TimeoutException, IOException {
    return processExecutor.execute();
  }

  @Override
  public StartedProcess tailLogsForPcf(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException {
    try {
      boolean loginSuccessful = pcfRequestConfig.isLoggedin()
          ? pcfRequestConfig.isLoggedin()
          : doLogin(pcfRequestConfig, executionLogCallback, pcfRequestConfig.getCfHomeDirPath());

      if (!loginSuccessful) {
        executionLogCallback.saveExecutionLog(color("Failed to login", Red, Bold));
        throw new PivotalClientApiException("Failed to login");
      }

      ProcessExecutor processExecutor = getProcessExecutorForLogTailing(pcfRequestConfig, executionLogCallback);

      return processExecutor.start();
    } catch (Exception e) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed while tailing logs", e);
    }
  }

  @VisibleForTesting
  ProcessExecutor getProcessExecutorForLogTailing(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback) {
    return new ProcessExecutor()
        .timeout(pcfRequestConfig.getTimeOutIntervalInMins(), TimeUnit.MINUTES)
        .command(BIN_BASH, "-c",
            CfCliCommandResolver.getLogsCommand(pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion(),
                pcfRequestConfig.getApplicationName()))
        .readOutput(true)
        .environment(
            getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(), pcfRequestConfig.getCfHomeDirPath()))
        .redirectOutput(new LogOutputStream() {
          @Override
          protected void processLine(String line) {
            executionLogCallback.saveExecutionLog(line);
          }
        });
  }

  @Override
  public void unmapRoutesForApplicationUsingCli(CfRequestConfig pcfRequestConfig, List<String> routes,
      LogCallback logCallback) throws PivotalClientApiException, InterruptedException {
    executeRoutesOperationForApplicationUsingCli(CfCliCommandType.UNMAP_ROUTE, pcfRequestConfig, routes, logCallback);
  }

  @VisibleForTesting
  PcfRouteInfo extractRouteInfoFromPath(Set<String> domainNames, String route) throws PivotalClientApiException {
    PcfRouteInfoBuilder builder = PcfRouteInfo.builder();
    int index = route.indexOf(PCF_ROUTE_PORT_SEPARATOR);
    if (index != -1) {
      // TCP
      builder.type(PCF_ROUTE_TYPE_TCP);
      String port = route.substring(index + 1);
      builder.port(port);
      String domain = route.substring(0, index);
      builder.domain(domain);
      return builder.build();
    }

    String path = null;
    String routeWithoutPath = route;
    int indexForPath = route.indexOf(PCF_ROUTE_PATH_SEPARATOR_CHAR);
    if (indexForPath != -1) {
      path = route.substring(indexForPath + 1);
      routeWithoutPath = route.substring(0, indexForPath);
    }
    builder.path(path);

    // HTTP
    builder.type(PCF_ROUTE_TYPE_HTTP);
    String domain = getDomain(domainNames, routeWithoutPath);

    if (domain == null) {
      throw new PivotalClientApiException(new StringBuilder(128)
                                              .append("Invalid Route Name: ")
                                              .append(route)
                                              .append(", used domain not present in this space")
                                              .toString());
    }
    builder.domain(domain);

    int domainStartIndex = route.indexOf(domain);
    String hostName = domainStartIndex == 0 ? null : route.substring(0, domainStartIndex - 1);
    builder.hostName(hostName);

    return builder.build();
  }

  @Override
  public void mapRoutesForApplicationUsingCli(
      CfRequestConfig pcfRequestConfig, List<String> routes, LogCallback logCallback) throws PivotalClientApiException {
    executeRoutesOperationForApplicationUsingCli(CfCliCommandType.MAP_ROUTE, pcfRequestConfig, routes, logCallback);
  }

  private String getDomain(Set<String> domains, String domain) {
    if (domains.contains(domain)) {
      return domain;
    } else if (domain.contains(".")) {
      return getDomain(domains, domain.substring(domain.indexOf('.') + 1));
    }
    return null;
  }

  @VisibleForTesting
  void executeRoutesOperationForApplicationUsingCli(CfCliCommandType commandType, CfRequestConfig pcfRequestConfig,
      List<String> routes, LogCallback logCallback) throws PivotalClientApiException {
    try {
      if (!pcfRequestConfig.isUseCFCLI()) {
        throw new InvalidRequestException("Trying to map routes using Cli without flag in Pcf request Config");
      }

      if (!pcfRequestConfig.isLoggedin()) {
        if (!doLogin(pcfRequestConfig, logCallback, pcfRequestConfig.getCfHomeDirPath())) {
          String errorMessage = format("Failed to login when performing: [%s]", commandType.toString());

          logCallback.saveExecutionLog(color(errorMessage, Red, Bold));
          throw new InvalidRequestException(errorMessage);
        }
        pcfRequestConfig.setLoggedin(true);
      }

      List<Domain> allDomainsForSpace = cfSdkClient.getAllDomainsForSpace(pcfRequestConfig);
      Set<String> domainNames = allDomainsForSpace.stream().map(Domain::getName).collect(toSet());
      logCallback.saveExecutionLog(format("Found domain names: [%s]", join(", ", domainNames)));

      if (isNotEmpty(routes)) {
        int exitcode;
        String command;
        Map<String, String> env =
            getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(), pcfRequestConfig.getCfHomeDirPath());
        for (String route : routes) {
          logCallback.saveExecutionLog(format("Extracting info from route: [%s]", route));
          PcfRouteInfo info = extractRouteInfoFromPath(domainNames, route);
          if (PCF_ROUTE_TYPE_TCP == info.getType()) {
            command = getRouteCommandForTcpType(commandType, pcfRequestConfig, info);
          } else {
            command = getRouteCommand(commandType, pcfRequestConfig, info);
          }
          exitcode = executeCommand(command, env, logCallback);
          if (exitcode != 0) {
            String message = format("Failed to map route: [%s]", route);
            logCallback.saveExecutionLog(message, ERROR);
            throw new InvalidRequestException(message);
          }
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed mapping routes", ex);
    } catch (IOException | TimeoutException ex) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed mapping routes", ex);
    }
  }

  private String getRouteCommandForTcpType(
      CfCliCommandType commandType, CfRequestConfig pcfRequestConfig, PcfRouteInfo info) {
    if (commandType == CfCliCommandType.UNMAP_ROUTE) {
      return CfCliCommandResolver.getUnmapRouteCommand(pcfRequestConfig.getCfCliPath(),
          pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getApplicationName(), info.getDomain(), info.getPort());
    } else if (commandType == CfCliCommandType.MAP_ROUTE) {
      return CfCliCommandResolver.getMapRouteCommand(pcfRequestConfig.getCfCliPath(),
          pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getApplicationName(), info.getDomain(), info.getPort());
    } else {
      throw new InvalidArgumentsException(format("Unsupported route command type, expected: %s, found: %s",
          Arrays.asList(CfCliCommandType.UNMAP_ROUTE, CfCliCommandType.MAP_ROUTE), commandType));
    }
  }

  private String getRouteCommand(CfCliCommandType commandType, CfRequestConfig pcfRequestConfig, PcfRouteInfo info) {
    if (commandType == CfCliCommandType.UNMAP_ROUTE) {
      return CfCliCommandResolver.getUnmapRouteCommand(pcfRequestConfig.getCfCliPath(),
          pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getApplicationName(), info.getDomain(),
          info.getHostName(), info.getPath());
    } else if (commandType == CfCliCommandType.MAP_ROUTE) {
      return CfCliCommandResolver.getMapRouteCommand(pcfRequestConfig.getCfCliPath(),
          pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getApplicationName(), info.getDomain(),
          info.getHostName(), info.getPath());
    } else {
      throw new InvalidArgumentsException(format("Unsupported route command type, expected: %s, found: %s",
          Arrays.asList(CfCliCommandType.UNMAP_ROUTE, CfCliCommandType.MAP_ROUTE), commandType));
    }
  }

  @Override
  public void setEnvVariablesForApplication(Map<String, Object> envVars, CfRequestConfig pcfRequestConfig,
      LogCallback logCallback) throws PivotalClientApiException {
    try {
      if (!pcfRequestConfig.isUseCFCLI()) {
        throw new InvalidRequestException("USE_PCF_CLI flag is needed");
      }

      if (!pcfRequestConfig.isLoggedin()) {
        if (!doLogin(pcfRequestConfig, logCallback, pcfRequestConfig.getCfHomeDirPath())) {
          String errorMessage = "Failed to login when performing: set-env";
          logCallback.saveExecutionLog(color(errorMessage, Red, Bold));
          throw new InvalidRequestException(errorMessage);
        }
      }

      if (isNotEmpty(envVars)) {
        int exitcode;
        String command;
        Map<String, String> env =
            getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(), pcfRequestConfig.getCfHomeDirPath());
        logCallback.saveExecutionLog(color(
            "\n # Set Environment Variables for Application: " + pcfRequestConfig.getApplicationName(), White, Bold));
        for (Map.Entry<String, Object> entry : envVars.entrySet()) {
          logCallback.saveExecutionLog(new StringBuilder(128)
                                           .append("Environment Variable- ")
                                           .append(entry.getKey())
                                           .append(": ")
                                           .append(entry.getValue())
                                           .toString());

          command =
              CfCliCommandResolver.getSetEnvCommand(pcfRequestConfig.getCfCliPath(), pcfRequestConfig.getCfCliVersion(),
                  pcfRequestConfig.getApplicationName(), entry.getKey(), String.valueOf(entry.getValue()));

          exitcode = executeCommand(command, env, logCallback);
          if (exitcode != 0) {
            String message = format("Failed to set env var: <%s>", entry.getKey() + ':' + entry.getValue());
            log.error(message);
            logCallback.saveExecutionLog(message, ERROR);
            throw new PivotalClientApiException(message);
          }
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed to set Env Variable", ex);
    } catch (IOException | TimeoutException ex) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed to set Env Variable", ex);
    }
  }

  @Override
  public void unsetEnvVariablesForApplication(List<String> varNames, CfRequestConfig pcfRequestConfig,
      LogCallback logCallback) throws PivotalClientApiException {
    try {
      if (!pcfRequestConfig.isUseCFCLI()) {
        throw new InvalidRequestException("USE_PCF_CLI flag is needed");
      }

      if (!pcfRequestConfig.isLoggedin()) {
        if (!doLogin(pcfRequestConfig, logCallback, pcfRequestConfig.getCfHomeDirPath())) {
          String errorMessage = "Failed to login when performing: set-env";
          logCallback.saveExecutionLog(color(errorMessage, Red, Bold));
          throw new InvalidRequestException(errorMessage);
        }
      }

      if (isNotEmpty(varNames)) {
        int exitcode;
        String command;
        Map<String, String> env =
            getEnvironmentMapForPcfExecutor(pcfRequestConfig.getEndpointUrl(), pcfRequestConfig.getCfHomeDirPath());
        logCallback.saveExecutionLog(color(
            "\n # Unset Environment Variables for Application: " + pcfRequestConfig.getApplicationName(), White, Bold));
        for (String var : varNames) {
          logCallback.saveExecutionLog(new StringBuilder(128).append("Environment Variable: ").append(var).toString());

          command = CfCliCommandResolver.getUnsetEnvCommand(pcfRequestConfig.getCfCliPath(),
              pcfRequestConfig.getCfCliVersion(), pcfRequestConfig.getApplicationName(), var);

          exitcode = executeCommand(command, env, logCallback);
          if (exitcode != 0) {
            String message = "Failed to unset env var: " + var;
            log.error(message);
            logCallback.saveExecutionLog(message, ERROR);
            throw new PivotalClientApiException(message);
          }
        }
      }
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed to set Env Variable", ex);
    } catch (IOException | TimeoutException ex) {
      throw new PivotalClientApiException(PIVOTAL_CLOUD_FOUNDRY_CLIENT_EXCEPTION + "Failed to set Env Variable", ex);
    }
  }
}
