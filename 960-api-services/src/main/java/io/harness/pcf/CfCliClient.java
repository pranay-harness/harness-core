package io.harness.pcf;

import io.harness.logging.LogCallback;
import io.harness.pcf.model.CfAppAutoscalarRequestData;
import io.harness.pcf.model.CfCreateApplicationRequestData;
import io.harness.pcf.model.CfRequestConfig;
import io.harness.pcf.model.CfRunPluginScriptRequestData;

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
  void pushAppByCli(CfCreateApplicationRequestData requestData, LogCallback executionLogCallback)
      throws PivotalClientApiException;

  /**
   * Configure Autoscaler service.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @throws PivotalClientApiException
   */
  void performConfigureAutoscaler(CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback executionLogCallback)
      throws PivotalClientApiException;

  /**
   * Change Autoscaler service state.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @param enable
   * @throws PivotalClientApiException
   */
  void changeAutoscalerState(CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback executionLogCallback,
      boolean enable) throws PivotalClientApiException;

  /**
   * Check whether Autoscaler service attached.
   *
   * @param appAutoscalarRequestData
   * @param executionLogCallback
   * @return boolean
   * @throws PivotalClientApiException
   */
  boolean checkIfAppHasAutoscalerAttached(CfAppAutoscalarRequestData appAutoscalarRequestData,
      LogCallback executionLogCallback) throws PivotalClientApiException;

  /**
   * Check whether Autoscaler service is in expected state.
   *
   * @param appAutoscalarRequestData
   * @param logCallback
   * @return boolean
   * @throws PivotalClientApiException
   */
  boolean checkIfAppHasAutoscalerWithExpectedState(
      CfAppAutoscalarRequestData appAutoscalarRequestData, LogCallback logCallback) throws PivotalClientApiException;

  /**
   * Unmap application routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @param logCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void unmapRoutesForApplicationUsingCli(CfRequestConfig pcfRequestConfig, List<String> routes, LogCallback logCallback)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Map application routes.
   *
   * @param pcfRequestConfig
   * @param routes
   * @param logCallback
   * @throws PivotalClientApiException
   * @throws InterruptedException
   */
  void mapRoutesForApplicationUsingCli(CfRequestConfig pcfRequestConfig, List<String> routes, LogCallback logCallback)
      throws PivotalClientApiException, InterruptedException;

  /**
   * Run plugin script.
   *
   * @param pcfRunPluginScriptRequestData
   * @param executionLogCallback
   * @throws PivotalClientApiException
   */
  void runPcfPluginScript(CfRunPluginScriptRequestData pcfRunPluginScriptRequestData, LogCallback executionLogCallback)
      throws PivotalClientApiException;

  /**
   * Set application env variables.
   *
   * @param envVars
   * @param pcfRequestConfig
   * @param logCallback
   * @throws PivotalClientApiException
   */
  void setEnvVariablesForApplication(Map<String, Object> envVars, CfRequestConfig pcfRequestConfig,
      LogCallback logCallback) throws PivotalClientApiException;

  /**
   * Unset application env variables.
   *
   * @param varNames
   * @param pcfRequestConfig
   * @param logCallback
   * @throws PivotalClientApiException
   */
  void unsetEnvVariablesForApplication(List<String> varNames, CfRequestConfig pcfRequestConfig, LogCallback logCallback)
      throws PivotalClientApiException;

  /**
   * Stream application logging information.
   *
   * @param pcfRequestConfig
   * @param executionLogCallback
   * @return {@link StartedProcess}
   * @throws PivotalClientApiException
   */
  StartedProcess tailLogsForPcf(CfRequestConfig pcfRequestConfig, LogCallback executionLogCallback)
      throws PivotalClientApiException;
}
