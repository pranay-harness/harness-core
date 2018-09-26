package software.wings.service.impl;

import static io.harness.beans.PageResponse.PageResponseBuilder.aPageResponse;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.ContainerService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.InfrastructureProvider;

import java.util.List;

/**
 * Created by brett on 2/27/17
 */
@Singleton
public class GcpInfrastructureProvider implements InfrastructureProvider {
  @Inject private HostService hostService;
  @Inject private DelegateProxyFactory delegateProxyFactory;

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

  List<String> listClusterNames(
      SettingAttribute computeProviderSetting, List<EncryptedDataDetail> encryptedDataDetails) {
    SyncTaskContext syncTaskContext = aContext().withAccountId(computeProviderSetting.getAccountId()).build();
    return delegateProxyFactory.get(ContainerService.class, syncTaskContext)
        .listClusters(ContainerServiceParams.builder()
                          .encryptionDetails(encryptedDataDetails)
                          .settingAttribute(computeProviderSetting)
                          .clusterName("None")
                          .build());
  }
}
