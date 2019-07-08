package software.wings.graphql.datafetcher.cloudProvider;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.beans.SettingAttribute.SettingCategory;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilterType;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection.QLCloudProviderConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Slf4j
public class CloudProviderConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCloudProviderFilter, QLNoOpSortCriteria, QLCloudProviderConnection> {
  @Inject private SettingsService settingsService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLCloudProviderConnection fetchConnection(List<QLCloudProviderFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<SettingAttribute> query = populateFilters(wingsPersistence, filters, SettingAttribute.class)
                                        .filter(SettingAttributeKeys.category, SettingCategory.CLOUD_PROVIDER);

    final List<SettingAttribute> settingAttributes = query.asList();

    final List<SettingAttribute> filteredSettingAttributes =
        settingsService.getFilteredSettingAttributes(settingAttributes, null, null);

    QLCloudProviderConnectionBuilder qlCloudProviderConnectionBuilder = QLCloudProviderConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder().hasMore(false).offset(0).limit(0).total(0);

    if (isNotEmpty(filteredSettingAttributes)) {
      pageInfoBuilder.total(filteredSettingAttributes.size()).limit(filteredSettingAttributes.size());

      for (SettingAttribute settingAttribute : filteredSettingAttributes) {
        qlCloudProviderConnectionBuilder.node(CloudProviderController
                                                  .populateCloudProvider(settingAttribute,
                                                      CloudProviderController.getCloudProviderBuilder(settingAttribute))
                                                  .build());
      }
    }

    return qlCloudProviderConnectionBuilder.build();
  }

  protected String getFilterFieldName(String filterType) {
    QLCloudProviderFilterType qlFilterType = QLCloudProviderFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Type:
        return "value.type";
      case CloudProvider:
        return "_id";
      case CreatedAt:
        return "createdAt";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
