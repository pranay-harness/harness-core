package software.wings.helpers.ext.helm;

import software.wings.beans.command.ExecutionLogCallback;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmReleaseHistoryCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmCommandResponse;
import software.wings.helpers.ext.helm.response.HelmListReleasesCommandResponse;
import software.wings.helpers.ext.helm.response.HelmReleaseHistoryCommandResponse;

/**
 * Created by anubhaw on 4/1/18.
 */
public interface HelmDeployService {
  /**
   * Deploy helm command response.
   *
   * @param commandRequest       the command request
   * @param executionLogCallback the execution log callback
   * @return the helm command response
   */
  HelmCommandResponse deploy(HelmInstallCommandRequest commandRequest, ExecutionLogCallback executionLogCallback);

  /**
   * Rollback helm command response.
   *
   * @param commandRequest       the command request
   * @param executionLogCallback the execution log callback
   * @return the helm command response
   */
  HelmCommandResponse rollback(HelmRollbackCommandRequest commandRequest, ExecutionLogCallback executionLogCallback);

  /**
   * Ensure helm cli and tiller installed helm command response.
   *
   * @param helmCommandRequest   the helm command request
   * @param executionLogCallback the execution log callback
   * @return the helm command response
   */
  HelmCommandResponse ensureHelmCliAndTillerInstalled(
      HelmCommandRequest helmCommandRequest, ExecutionLogCallback executionLogCallback);

  /**
   * Last successful release version string.
   *
   * @param helmCommandRequest the helm command request
   * @return the string
   */
  HelmListReleasesCommandResponse listReleases(HelmInstallCommandRequest helmCommandRequest);

  /**
   * Release history helm release history command response.
   *
   * @param helmCommandRequest the helm command request
   * @return the helm release history command response
   */
  HelmReleaseHistoryCommandResponse releaseHistory(HelmReleaseHistoryCommandRequest helmCommandRequest);
}
