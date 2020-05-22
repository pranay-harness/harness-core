package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.PhysicalDataCenterConfig.Builder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPhysicalDataCenterCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.settings.SettingValue.SettingVariableTypes;

@Singleton
public class PhysicalDataCenterDataFetcherHelper {
  @Inject private UsageScopeController usageScopeController;

  public SettingAttribute toSettingAttribute(QLPhysicalDataCenterCloudProviderInput input, String accountId) {
    SettingAttribute.Builder settingAttributeBuilder =
        SettingAttribute.Builder.aSettingAttribute()
            .withValue(
                Builder.aPhysicalDataCenterConfig().withType(SettingVariableTypes.PHYSICAL_DATA_CENTER.name()).build())
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
      SettingAttribute settingAttribute, QLPhysicalDataCenterCloudProviderInput input, String accountId) {
    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
