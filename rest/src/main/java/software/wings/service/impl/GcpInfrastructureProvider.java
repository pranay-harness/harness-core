package software.wings.service.impl;

import static software.wings.dl.PageResponse.Builder.aPageResponse;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;

import java.util.List;

/**
 * Created by brett on 2/27/17
 */
@Singleton
public class GcpInfrastructureProvider implements InfrastructureProvider {
  private static final int SLEEP_INTERVAL = 30 * 1000;
  private static final int RETRY_COUNTER = (10 * 60 * 1000) / SLEEP_INTERVAL; // 10 minutes

  private final Logger logger = LoggerFactory.getLogger(GcpInfrastructureProvider.class);

  @Inject private GkeClusterService gkeClusterService;
  @Inject private HostService hostService;

  @Override
  public PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
    return aPageResponse().withResponse(null).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String dnsName) {
    hostService.deleteByDnsName(appId, infraMappingId, dnsName);
  }

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    hostService.updateHostConnectionAttrByInfraMappingId(
        infrastructureMapping.getAppId(), infrastructureMapping.getUuid(), hostConnectionAttrs);
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
  }

  public List<String> listClusterNames(
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    return gkeClusterService.listClusters(computeProviderSetting, encryptedDataDetails);
  }
}
