package software.wings.service.impl;

import com.google.inject.Singleton;

import software.wings.beans.AwsInstanceFilter;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;

import javax.inject.Inject;

/**
 * Created by anubhaw on 1/12/17.
 */
@Singleton
public class StaticInfrastructureProvider implements InfrastructureProvider {
  @Inject private HostService hostService;

  @Override
  public PageResponse<Host> listHosts(String region, SettingAttribute computeProviderSetting,
      AwsInstanceFilter instanceFilter, boolean usePublicDns, PageRequest<Host> req) {
    return hostService.list(req);
  }

  @Override
  public Host saveHost(Host host) {
    return hostService.saveHost(host);
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
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {
    hostService.deleteByInfraMappingId(appId, infraMappingId);
  }
}
