/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.helm;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.helm.HelmClientImpl.HelmCliResponse;
import io.harness.k8s.model.HelmVersion;

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
   * @param helmCommandData the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmCliResponse install(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException, ExecutionException;

  /**
   * Upgrade helm command response.
   *
   * @param helmCommandData the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmCliResponse upgrade(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException, ExecutionException;

  /**
   * Rollback helm command response.
   *
   * @param helmCommandData the command request
   * @return the helm command response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmCliResponse rollback(HelmCommandData helmCommandData) throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse releaseHistory(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * List releases helm cli response.
   *
   * @param helmCommandData the command request
   * @return the helm cli response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmCliResponse listReleases(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * Gets client and server version.
   *
   * @param helmCommandData the helm command request
   * @return the client and server version
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   */
  HelmCliResponse getClientAndServerVersion(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse addPublicRepo(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse getHelmRepoList(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse deleteHelmRelease(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse repoUpdate(HelmCommandData helmCommandData)
      throws InterruptedException, TimeoutException, IOException;

  HelmCliResponse templateForK8sV2(String releaseName, String namespace, String chartLocation,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException;

  HelmCliResponse searchChart(HelmCommandData helmCommandData, String chartInfo)
      throws InterruptedException, TimeoutException, IOException;

  /**
   * Render chart templates and return the output.
   *
   * @param helmCommandData the command request
   * @param chartLocation
   * @param namespace
   * @param valuesOverrides
   * @return HelmCliResponse the helm cli response
   * @throws InterruptedException the interrupted exception
   * @throws TimeoutException     the timeout exception
   * @throws IOException          the io exception
   * @throws ExecutionException   the execution exception
   */
  HelmCliResponse renderChart(HelmCommandData helmCommandData, String chartLocation, String namespace,
      List<String> valuesOverrides) throws InterruptedException, TimeoutException, IOException, ExecutionException;

  String getHelmPath(HelmVersion helmVersion);
}
