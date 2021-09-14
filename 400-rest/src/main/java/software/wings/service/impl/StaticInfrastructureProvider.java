/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;

/**
 * Created by anubhaw on 1/12/17.
 */
@Singleton
public class StaticInfrastructureProvider implements InfrastructureProvider {
  @Inject private HostService hostService;

  @Override
  public PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
    return hostService.list(req);
  }

  @Override
  public PageResponse<Host> listHosts(InfrastructureDefinition infrastructureDefinition,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
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
    hostService.updateHostConnectionAttrByInfraMapping(infrastructureMapping, hostConnectionAttrs);
  }
}
