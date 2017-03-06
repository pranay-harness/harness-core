package software.wings.service.impl;

import com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsException;
import software.wings.service.intfc.InfrastructureProvider;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.List;

import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;

/**
 * Created by bzane on 2/27/17
 * TODO(brett): Implement
 */
@Singleton
public class GcpInfrastructureProvider implements InfrastructureProvider {
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes

  private final Logger logger = LoggerFactory.getLogger(GcpInfrastructureProvider.class);

  @Inject private GcpHelperService gcpHelperService;
  @Inject private GkeClusterService gkeClusterService;

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    GcpConfig gcpConfig = validateAndGetGcpConfig(computeProviderSetting);
    // TODO(brett): Implement
    return PageResponse.Builder.aPageResponse().withResponse(null).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String hostName) {
    // TODO(brett): Implement
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    // TODO(brett): Implement
  }

  @Override
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {
    // TODO(brett): Implement
  }

  private GcpConfig validateAndGetGcpConfig(SettingAttribute computeProviderSetting) {
    if (computeProviderSetting == null || !(computeProviderSetting.getValue() instanceof GcpConfig)) {
      throw new WingsException(INVALID_ARGUMENT, "message", "InvalidConfiguration");
    }

    return (GcpConfig) computeProviderSetting.getValue();
  }

  @Override
  public Host saveHost(Host host) {
    // TODO(brett): Implement
    return null;
  }

  public List<Host> provisionHosts(
      SettingAttribute computeProviderSetting, String launcherConfigName, int instanceCount) {
    GcpConfig gcpConfig = validateAndGetGcpConfig(computeProviderSetting);
    // TODO(brett): Implement
    return null;
  }

  public void deProvisionHosts(
      String appId, String infraMappingId, SettingAttribute computeProviderSetting, List<String> hostNames) {
    GcpConfig gcpConfig = validateAndGetGcpConfig(computeProviderSetting);
    // TODO(brett): Implement
  }

  public List<String> listClusterNames(SettingAttribute computeProviderSetting) {
    GcpConfig gcpConfig = validateAndGetGcpConfig(computeProviderSetting);
    return gkeClusterService.listClusters(ImmutableMap.of(
        "credentials", gcpConfig.getServiceAccountKeyFileContent(), "appName", computeProviderSetting.getAppId()));
  }
}
