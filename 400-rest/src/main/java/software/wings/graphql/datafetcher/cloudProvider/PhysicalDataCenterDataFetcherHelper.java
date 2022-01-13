/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.cloudProvider;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.PhysicalDataCenterConfig.Builder;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.secrets.UsageScopeController;
import software.wings.graphql.schema.mutation.cloudProvider.QLPhysicalDataCenterCloudProviderInput;
import software.wings.graphql.schema.mutation.cloudProvider.QLUpdatePhysicalDataCenterCloudProviderInput;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.settings.SettingVariableTypes;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
      SettingAttribute settingAttribute, QLUpdatePhysicalDataCenterCloudProviderInput input, String accountId) {
    if (input.getName().isPresent()) {
      input.getName().getValue().ifPresent(settingAttribute::setName);
    }

    if (input.getUsageScope().isPresent()) {
      QLUsageScope usageScope = input.getUsageScope().getValue().orElse(null);
      settingAttribute.setUsageRestrictions(usageScopeController.populateUsageRestrictions(usageScope, accountId));
    }
  }
}
