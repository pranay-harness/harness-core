package io.harness.pcf;

import io.harness.pcf.model.PcfAppAutoscalarRequestData;
import io.harness.pcf.model.PcfRequestConfig;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.pcf.request.PcfCreateApplicationRequestData;
import software.wings.helpers.ext.pcf.request.PcfRunPluginScriptRequestData;

import java.util.List;
import java.util.Map;
import org.zeroturnaround.exec.StartedProcess;

public interface CfCliClient {
  /**
   * Push application.
   *
   * @param requestData
   * @param executionLogCallback
   * @throws PivotalClientApiException
   */
  void pushAppByCli(PcfCreateApplicationRequestData requestData, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;

  /**
   * Configure Autoscaler service.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @throws PivotalClientApiException
   */
  void performConfigureAutoscaler(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  /**
   * Change Autoscaler service state.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @param enable
   * @throws PivotalClientApiException
   */
  void changeAutoscalerState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback, boolean enable) throws PivotalClientApiException;

  /**
   * Check whether Autoscaler service attached.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @return
   * @throws PivotalClientApiException
   */
  boolean checkIfAppHasAutoscalerAttached(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  /**
   * Check whether Autoscaler service is in expected state.
   *
   * @param appAutoscalarRequestData
   * @param logCallback
   * @return
   * @throws PivotalClientApiException
   */
  boolean checkIfAppHasAutoscalerWithExpectedState(PcfAppAutoscalarRequestData appAutoscalarRequestData,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  /**
   * Unmap application routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @param logCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException, InterruptedException;

  /**
   * Map application routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @param logCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRoutesForApplicationUsingCli(PcfRequestConfig pcfRequestConfig, List<String> routes,
      ExecutionLogCallback logCallback) throws PivotalClientApiException, InterruptedException;

  /**
   * Run plugin script.
   *
   * @param pcfRunPluginScriptRequestData
   * @param executionLogCallback
   * @throws PivotalClientApiException
   */
  void runPcfPluginScript(PcfRunPluginScriptRequestData pcfRunPluginScriptRequestData,
      ExecutionLogCallback executionLogCallback) throws PivotalClientApiException;

  /**
   * Set application env variables.
   *
   * @param envVars
   * @param pcfRequestConfig
   * @param logCallback
   * @throws PivotalClientApiException
   */
  void setEnvVariablesForApplication(Map<String, Object> envVars, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  /**
   * Unset application env variables.
   *
   * @param varNames
   * @param pcfRequestConfig
   * @param logCallback
   * @throws PivotalClientApiException
   */
  void unsetEnvVariablesForApplication(List<String> varNames, PcfRequestConfig pcfRequestConfig,
      ExecutionLogCallback logCallback) throws PivotalClientApiException;

  /**
   * Stream application logging information.
   *
   * @param pcfRequestConfig
   * @param executionLogCallback
   * @return
   * @throws PivotalClientApiException
   */
  StartedProcess tailLogsForPcf(PcfRequestConfig pcfRequestConfig, ExecutionLogCallback executionLogCallback)
      throws PivotalClientApiException;
}
