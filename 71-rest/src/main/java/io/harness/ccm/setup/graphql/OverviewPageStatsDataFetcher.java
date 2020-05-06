package io.harness.ccm.setup.graphql;

import com.google.inject.Inject;

import io.harness.persistence.HPersistence;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLNoOpQueryParameters;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.List;

public class OverviewPageStatsDataFetcher
    extends AbstractObjectDataFetcher<QLCEOverviewStatsData, QLNoOpQueryParameters> {
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLCEOverviewStatsData fetch(QLNoOpQueryParameters parameters, String accountId) {
    List<SettingAttribute> ceConnectorsList = persistence.createQuery(SettingAttribute.class)
                                                  .field(SettingAttributeKeys.accountId)
                                                  .equal(accountId)
                                                  .field(SettingAttributeKeys.category)
                                                  .equal(SettingCategory.CE_CONNECTOR.toString())
                                                  .asList();

    return QLCEOverviewStatsData.builder().cloudConnectorsPresent(!ceConnectorsList.isEmpty()).build();
  }
}
