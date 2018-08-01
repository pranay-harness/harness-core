package software.wings.helpers.ext.helm;

import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 3/22/18.
 */
public interface HelmClient {
  /**
   * Install helm command response.
   *
   * @param commandRequest the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmInstallCommandResponse install(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException, ExecutionException;

  /**
   * Upgrade helm command response.
   *
   * @param commandRequest the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmInstallCommandResponse upgrade(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException, ExecutionException;

  /**
   * Rollback helm command response.
   *
   * @param commandRequest the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmInstallCommandResponse rollback(HelmRollbackCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * Release history helm cli response.
   *
   * @param kubeConfigLocation the kube config location
   * @param releaseName        the release name
   * @return the helm cli response
   */
  HelmCliResponse releaseHistory(String kubeConfigLocation, String releaseName)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * List releases helm cli response.
   *
   * @param commandRequest the command request
   * @return the helm cli response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmCliResponse listReleases(HelmInstallCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * Gets client and server version.
   *
   * @param helmCommandRequest the helm command request
   * @return the client and server version
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmCliResponse getClientAndServerVersion(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse addPublicRepo(HelmCommandRequest helmCommandRequest)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse getHelmRepoList(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse deleteHelmRelease(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException;
}
