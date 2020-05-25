package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureConfig.AzureConfigBuilder;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.Builder;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLAzureCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;

@Singleton
public class AzureDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLAzureCloudProviderInput input, String accountId) {
    AzureConfigBuilder configBuilder = AzureConfig.builder().accountId(accountId);

    if (input.getClientId().isPresent() && input.getClientId().getValue().isPresent()) {
      input.getClientId().getValue().ifPresent(configBuilder::clientId);
    } else {
      throw new InvalidRequestException("No clientId input provided with the request.");
    }
    if (input.getTenantId().isPresent() && input.getTenantId().getValue().isPresent()) {
      input.getTenantId().getValue().ifPresent(configBuilder::tenantId);
    } else {
      throw new InvalidRequestException("No tenantId input provided with the request.");
    }
    if (input.getKeySecretId().isPresent() && input.getKeySecretId().getValue().isPresent()) {
      input.getKeySecretId().getValue().ifPresent(configBuilder::encryptedKey);
    } else {
      throw new InvalidRequestException("No keySecretId input provided with the request.");
    }

    Builder settingAttributeBuilder = Builder.aSettingAttribute()
                                          .withValue(configBuilder.build())
                                          .withAccountId(accountId)
                                          .withCategory(SettingAttribute.SettingCategory.SETTING);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttributeBuilder::withName);
    }

    if (input.getUsageScope().isPresent()) {
      settingAttributeBuilder.withUsageRestrictions(
          usageScopeController.populateUsageRestrictions(input.getUsageScope().getValue().orElse(null), accountId));
    }

    return settingAttributeBuilder.build();
  }

  public void updateSettingAttribute(
      SettingAttribute settingAttribute, QLAzureCloudProviderInput input, String accountId) {
    AzureConfig config = (AzureConfig) settingAttribute.getValue();

    if (input.getClientId().isPresent()) {
      input.getClientId().getValue().ifPresent(config::setClientId);
    }
    if (input.getTenantId().isPresent()) {
      input.getTenantId().getValue().ifPresent(config::setTenantId);
    }
    if (input.getKeySecretId().isPresent()) {
      input.getKeySecretId().getValue().ifPresent(config::setEncryptedKey);
    }
    settingAttribute.setValue(config);

    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
