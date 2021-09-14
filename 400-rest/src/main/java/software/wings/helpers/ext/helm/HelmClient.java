/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.k8s.model.HelmVersion;

import software.wings.helpers.ext.helm.HelmClientImpl.HelmCliResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmInstallCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.helm.response.HelmInstallCommandResponse;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * Created by anubhaw on 3/22/18.
 */
@TargetModule(HarnessModule._960_API_SERVICES)
@OwnedBy(CDP)
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

  HelmCliResponse releaseHistory(HelmCommandRequest helmCommandRequest)
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

  HelmCliResponse repoUpdate(HelmCommandRequest commandRequest)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse templateForK8sV2(String releaseName, String namespace, String chartLocation,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException;

  HelmCliResponse searchChart(HelmInstallCommandRequest commandRequest, String chartInfo)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * Render chart templates and return the output.
   *
   * @param commandRequest the command request
   * @param chartLocation
   * @param namespace
   * @param valuesOverrides
   * @return HelmCliResponse the helm cli response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmCliResponse renderChart(HelmCommandRequest commandRequest, String chartLocation, String namespace,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException;

  String getHelmPath(HelmVersion helmVersion);
}
