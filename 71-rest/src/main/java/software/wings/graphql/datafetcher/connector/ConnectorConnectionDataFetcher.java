package software.wings.graphql.datafetcher.connector;

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
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilter;
import software.wings.graphql.schema.type.aggregation.connector.QLConnectorFilterType;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection;
import software.wings.graphql.schema.type.connector.QLConnectorsConnection.QLConnectorsConnectionBuilder;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SettingsService;

import java.util.List;

@Slf4j
public class ConnectorConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLConnectorFilter, QLNoOpSortCriteria, QLConnectorsConnection> {
  @Inject private SettingsService settingsService;

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLConnectorsConnection fetchConnection(List<QLConnectorFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<SettingAttribute> query = populateFilters(wingsPersistence, filters, SettingAttribute.class)
                                        .filter(SettingAttributeKeys.category, SettingCategory.CONNECTOR);

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

  protected String getFilterFieldName(String filterType) {
    QLConnectorFilterType qlFilterType = QLConnectorFilterType.valueOf(filterType);
    switch (qlFilterType) {
      case Type:
        return "value.type";
      case Connector:
        return "_id";
      case CreatedAt:
        return "createdAt";
      default:
        throw new WingsException("Unknown filter type" + filterType);
    }
  }
}
