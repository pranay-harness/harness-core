package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.PcfConfig;
import software.wings.beans.PcfConfig.PcfConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPcfCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePcfCloudProviderInput;

@Singleton
public class PcfDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLPcfCloudProviderInput input, String accountId) {
    PcfConfigBuilder pcfConfigBuilder = PcfConfig.builder().accountId(accountId);

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfigBuilder::endpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(pcfConfigBuilder::username);
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfigBuilder::encryptedPassword);
    }
    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(pcfConfigBuilder::skipValidation);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(pcfConfigBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLUpdatePcfCloudProviderInput input, String accountId) {
    PcfConfig pcfConfig = (PcfConfig) settingAttribute.getValue();

    if (input.getEndpointUrl().isPresent()) {
      input.getEndpointUrl().getValue().ifPresent(pcfConfig::setEndpointUrl);
    }
    if (input.getUserName().isPresent()) {
      input.getUserName().getValue().ifPresent(pcfConfig::setUsername);
    }
    if (input.getPasswordSecretId().isPresent()) {
      input.getPasswordSecretId().getValue().ifPresent(pcfConfig::setEncryptedPassword);
    }
    if (input.getSkipValidation().isPresent()) {
      input.getSkipValidation().getValue().ifPresent(pcfConfig::setSkipValidation);
    }
    settingAttribute.setValue(pcfConfig);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }
  }
}
