package software.wings.service.impl;

import static software.wings.dl.PageResponse.Builder.aPageResponse;

import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.InfrastructureProvider;

import javax.inject.Singleton;

/**
 * Created by brett on 6/27/17
 */
@Singleton
public class DirectInfrastructureProvider implements InfrastructureProvider {
  @Override
  public PageResponse<Host> listHosts(String region, SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    return aPageResponse().withResponse(null).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String hostName) {}

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {}

  @Override
  public void deleteHostByInfraMappingId(String appId, String infraMappingId) {}

  @Override
  public Host saveHost(Host host) {
    return null;
  }
}
