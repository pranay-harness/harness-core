package software.wings.graphql.datafetcher.cloudProvider;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
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
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.cloudprovider.QLCloudProviderFilter;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProvider;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection;
import software.wings.graphql.schema.type.cloudProvider.QLCloudProviderConnection.QLCloudProviderConnectionBuilder;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CloudProviderConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLCloudProviderFilter, QLNoOpSortCriteria, QLCloudProviderConnection> {
  @Inject private SettingsService settingsService;
  @Inject private CloudProviderQueryHelper cloudProviderQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLCloudProviderConnection fetchConnection(List<QLCloudProviderFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<SettingAttribute> query = populateFilters(wingsPersistence, filters, SettingAttribute.class, true)
                                        .filter(SettingAttributeKeys.category, SettingCategory.CLOUD_PROVIDER);

    final List<SettingAttribute> settingAttributes = query.asList();

    int offset = pageQueryParameters.getOffset();
    int limit = pageQueryParameters.getLimit();

    List<SettingAttribute> filteredSettingAttributes =
        settingsService.getFilteredSettingAttributes(settingAttributes, null, null);

    QLCloudProviderConnectionBuilder qlCloudProviderConnectionBuilder = QLCloudProviderConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder();

    if (filteredSettingAttributes == null) {
      filteredSettingAttributes = new ArrayList<>();
    }

    List<SettingAttribute> resp;
    int total = filteredSettingAttributes.size();
    if (total <= offset) {
      resp = new ArrayList<>();
    } else {
      int endIdx = Math.min(offset + limit, total);
      resp = filteredSettingAttributes.subList(offset, endIdx);
    }

    List<QLCloudProvider> nodes = new ArrayList<>();
    for (SettingAttribute settingAttribute : resp) {
      nodes.add(CloudProviderController
                    .populateCloudProvider(
                        settingAttribute, CloudProviderController.getCloudProviderBuilder(settingAttribute))
                    .build());
    }

    QLPageInfo pageInfo = pageInfoBuilder.total(total).limit(limit).offset(offset).hasMore(total > offset).build();
    qlCloudProviderConnectionBuilder.pageInfo(pageInfo).nodes(nodes);

    return qlCloudProviderConnectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLCloudProviderFilter> filters, Query query) {
    cloudProviderQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLCloudProviderFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    if (NameService.cloudProvider.equals(key)) {
      return QLCloudProviderFilter.builder()
          .cloudProvider(QLIdFilter.builder()
                             .operator(QLIdOperator.EQUALS)
                             .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                             .build())
          .build();
    }
    throw new WingsException("Unsupported field " + key + " while generating filter");
  }
}
