package software.wings.graphql.datafetcher.connector;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
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
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilter;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection.QLConnectorsConnectionBuilder;
import software.wings.graphql.utils.nameservice.NameService;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Slf4j
public class ConnectorConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLConnectorFilter, QLNoOpSortCriteria, QLConnectorsConnection> {
  @Inject private SettingsService settingsService;
  @Inject ConnectorQueryHelper connectorQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLConnectorsConnection fetchConnection(List<QLConnectorFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    final Iterable<SettingCategory> settingValues =
        Lists.newArrayList(SettingCategory.CONNECTOR, SettingCategory.HELM_REPO);
    Query<SettingAttribute> query = populateFilters(wingsPersistence, filters, SettingAttribute.class);
    query = query.field(SettingAttributeKeys.category).in(settingValues);

    final List<SettingAttribute> settingAttributes = query.asList();

    final List<SettingAttribute> filteredSettingAttributes =
        settingsService.getFilteredSettingAttributes(settingAttributes, null, null);

    QLConnectorsConnectionBuilder connectorsConnectionBuilder = QLConnectorsConnection.builder();

    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder().hasMore(false).offset(0).limit(0).total(0);

    if (isNotEmpty(filteredSettingAttributes)) {
      pageInfoBuilder.total(filteredSettingAttributes.size()).limit(filteredSettingAttributes.size());

      for (SettingAttribute settingAttribute : filteredSettingAttributes) {
        connectorsConnectionBuilder.node(
            ConnectorsController
                .populateConnector(settingAttribute, ConnectorsController.getConnectorBuilder(settingAttribute))
                .build());
      }
    }
    return connectorsConnectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLConnectorFilter> filters, Query query) {
    connectorQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLConnectorFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    if (NameService.connector.equals(key)) {
      return QLConnectorFilter.builder()
          .connector(QLIdFilter.builder()
                         .operator(QLIdOperator.EQUALS)
                         .values(new String[] {(String) utils.getFieldValue(environment.getSource(), value)})
                         .build())
          .build();
    }
    throw new WingsException("Unsupported field " + key + " while generating filter");
  }
}
