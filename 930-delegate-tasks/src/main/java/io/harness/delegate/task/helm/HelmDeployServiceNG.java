package io.harness.delegate.task.helm;

import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;

import java.util.List;

public interface HelmDeployServiceNG {
  void setLogStreamingClient(ILogStreamingTaskClient iLogStreamingTaskClient);
  HelmCommandResponseNG deploy(HelmInstallCommandRequestNG commandRequest) throws Exception;

  /**
   * Rollback helm command response.
   *
   * @param commandRequest       the command request
   * @return the helm command response
   */
  HelmCommandResponseNG rollback(HelmRollbackCommandRequestNG commandRequest);

  /**
   * Ensure helm cli and tiller installed helm command response.
   *
   * @param helmCommandRequest   the helm command request
   * @return the helm command response
   */
  HelmCommandResponseNG ensureHelmCliAndTillerInstalled(HelmCommandRequestNG helmCommandRequest);

  /**
   * Last successful release version string.
   *
   * @param helmCommandRequest the helm command request
   * @return the string
   */
  HelmListReleaseResponseNG listReleases(HelmInstallCommandRequestNG helmCommandRequest) throws Exception;

  /**
   * Release history helm release history command response.
   *
   * @param helmCommandRequest the helm command request
   * @return the helm release history command response
   */
  HelmReleaseHistoryCmdResponseNG releaseHistory(HelmReleaseHistoryCommandRequestNG helmCommandRequest);

  /**
   * Render chart templates and return the output.
   *
   * @param helmCommandRequest the helm command request
   * @param namespace the namespace
   * @param chartLocation the chart location
   * @param valueOverrides the value overrides
   * @return the helm release history command response
   */
  HelmCommandResponseNG renderHelmChart(HelmCommandRequestNG helmCommandRequest, String namespace, String chartLocation,
      List<String> valueOverrides) throws Exception;

  HelmCommandResponseNG ensureHelm3Installed(HelmCommandRequestNG commandRequest);

  HelmCommandResponseNG ensureHelmInstalled(HelmCommandRequestNG commandRequest);
}
