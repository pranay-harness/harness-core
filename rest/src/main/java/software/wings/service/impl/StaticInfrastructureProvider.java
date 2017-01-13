package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;

/**
 * Created by anubhaw on 1/12/17.
 */
@Singleton
public class StaticInfrastructureProvider implements InfrastructureProvider {
  @Inject private HostService hostService;

  @Override
  public PageResponse<Host> listHosts(SettingAttribute computeProviderSetting, PageRequest<Host> req) {
    return hostService.list(req);
  }

  @Override
  public Host saveHost(SettingAttribute computeProviderSetting, Host host) {
    return hostService.saveHost(host);
  }
}
