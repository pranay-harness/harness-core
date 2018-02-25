package software.wings.service.impl;

import static software.wings.dl.PageResponse.PageResponseBuilder.aPageResponse;

import com.google.inject.Singleton;

import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.InfrastructureProvider;

import java.util.List;

/**
 * Created by brett on 6/27/17
 */
@Singleton
public class DirectInfrastructureProvider implements InfrastructureProvider {
  @Override
  public PageResponse<Host> listHosts(AwsInfrastructureMapping awsInfrastructureMapping,
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails, PageRequest<Host> req) {
    return aPageResponse().withResponse(null).build();
  }

  @Override
  public void deleteHost(String appId, String infraMappingId, String dnsName) {}

  @Override
  public void updateHostConnAttrs(InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {}

  @Override
  public Host saveHost(Host host) {
    return null;
  }
}
