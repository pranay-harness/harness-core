package software.wings.service.impl.yaml.handler.InfraDefinition;

import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.VMSSAuthType.PASSWORD;
import static software.wings.beans.VMSSAuthType.SSH_PUBLIC_KEY;
import static software.wings.infra.AzureVMSSInfra.Yaml;

import com.google.inject.Inject;

import software.wings.beans.InfrastructureType;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.infra.AzureVMSSInfra;
import software.wings.service.impl.yaml.handler.CloudProviderInfrastructure.CloudProviderInfrastructureYamlHandler;
import software.wings.service.intfc.SettingsService;

import java.util.List;

public class AzureVMSSInfraYamlHandler extends CloudProviderInfrastructureYamlHandler<Yaml, AzureVMSSInfra> {
  @Inject private SettingsService settingsService;

  @Override
  public Yaml toYaml(AzureVMSSInfra bean, String appId) {
    SettingAttribute cloudProvider = settingsService.get(bean.getCloudProviderId());
    notNullCheck("Cloud provider is NULL", cloudProvider);
    String hostConnectionAttributeName = null;
    String passwordName = null;
    if (SSH_PUBLIC_KEY == bean.getVmssAuthType()) {
      SettingAttribute attribute = settingsService.get(bean.getHostConnectionAttrs());
      notNullCheck("Public Key is NULL", attribute);
      hostConnectionAttributeName = attribute.getName();
    } else if (PASSWORD == bean.getVmssAuthType()) {
      SettingAttribute attribute = settingsService.get(bean.getPassword());
      notNullCheck("Password is NULL", attribute);
      passwordName = attribute.getName();
    }

    return Yaml.builder()
        .type(InfrastructureType.AZURE_VMSS)
        .cloudProviderName(cloudProvider.getName())
        .baseVMSSName(bean.getBaseVMSSName())
        .userName(bean.getUserName())
        .resourceGroupName(bean.getResourceGroupName())
        .subscriptionId(bean.getSubscriptionId())
        .password(passwordName)
        .hostConnectionAttrs(hostConnectionAttributeName)
        .vmssAuthType(bean.getVmssAuthType())
        .vmssDeploymentType(bean.getVmssDeploymentType())
        .build();
  }

  @Override
  public AzureVMSSInfra upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    AzureVMSSInfra bean = AzureVMSSInfra.builder().build();
    toBean(bean, changeContext);
    return bean;
  }

  private void toBean(AzureVMSSInfra bean, ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    SettingAttribute cloudProvider = settingsService.getSettingAttributeByName(accountId, yaml.getCloudProviderName());
    notNullCheck("Cloud Provider is NULL", cloudProvider);
    String hostConnectionAttributeId = null;
    String passwordId = null;
    if (SSH_PUBLIC_KEY == yaml.getVmssAuthType()) {
      SettingAttribute attribute = settingsService.getSettingAttributeByName(accountId, yaml.getHostConnectionAttrs());
      notNullCheck("Public Key is NULL", attribute);
      hostConnectionAttributeId = attribute.getUuid();
    } else if (PASSWORD == yaml.getVmssAuthType()) {
      SettingAttribute attribute = settingsService.getSettingAttributeByName(accountId, yaml.getPassword());
      notNullCheck("Password is NULL", attribute);
      passwordId = attribute.getUuid();
    }
    bean.setCloudProviderId(cloudProvider.getUuid());
    bean.setHostConnectionAttrs(hostConnectionAttributeId);
    bean.setPassword(passwordId);
    bean.setBaseVMSSName(yaml.getBaseVMSSName());
    bean.setUserName(yaml.getUserName());
    bean.setResourceGroupName(yaml.getResourceGroupName());
    bean.setSubscriptionId(yaml.getSubscriptionId());
    bean.setVmssAuthType(yaml.getVmssAuthType());
    bean.setVmssDeploymentType(yaml.getVmssDeploymentType());
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }
}