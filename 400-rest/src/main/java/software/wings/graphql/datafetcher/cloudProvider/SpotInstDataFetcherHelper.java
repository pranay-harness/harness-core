package software.wings.graphql.datafetcher.cloudProvider;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SpotInstConfig;
import software.wings.beans.SpotInstConfig.SpotInstConfigBuilder;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLSpotInstCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdateSpotInstCloudProviderInput;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class SpotInstDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLSpotInstCloudProviderInput input, String accountId) {
    SpotInstConfigBuilder configBuilder = SpotInstConfig.builder().accountId(accountId);

    if (input.getAccountId().isPresent()) {
      input.getAccountId().getValue().ifPresent(configBuilder::spotInstAccountId);
    }
    if (input.getTokenSecretId().isPresent()) {
      input.getTokenSecretId().getValue().ifPresent(configBuilder::encryptedSpotInstToken);
    }

    SettingAttribute.Builder settingAttributeBuilder = SettingAttribute.Builder.aSettingAttribute()
                                                           .withValue(configBuilder.build())
                                                           .withAccountId(accountId)
                                                           .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLUpdateSpotInstCloudProviderInput input, String accountId) {
    SpotInstConfig config = (SpotInstConfig) settingAttribute.getValue();

    if (input.getAccountId().isPresent()) {
      input.getAccountId().getValue().ifPresent(config::setSpotInstAccountId);
    }
    if (input.getTokenSecretId().isPresent()) {
      input.getTokenSecretId().getValue().ifPresent(config::setEncryptedSpotInstToken);
    }
    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }
  }
}
