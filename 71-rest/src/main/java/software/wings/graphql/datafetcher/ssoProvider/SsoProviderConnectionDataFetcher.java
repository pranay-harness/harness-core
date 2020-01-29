package software.wings.graphql.datafetcher.ssoProvider;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOType;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLPageInfo;
import software.wings.graphql.schema.type.QLPageInfo.QLPageInfoBuilder;
import software.wings.graphql.schema.type.QLSSOProvider;
import software.wings.graphql.schema.type.QLSSOProvider.QLSSOProviderBuilder;
import software.wings.graphql.schema.type.QLSSOProviderConnection;
import software.wings.graphql.schema.type.QLSSOProviderConnection.QLSSOProviderConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.ssoProvider.QLSSOProviderFilter;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.SSOService;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SsoProviderConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLSSOProviderFilter, QLNoOpSortCriteria, QLSSOProviderConnection> {
  @Inject SSOService ssoService;
  @Override
  @AuthRule(permissionType = PermissionType.ACCOUNT_MANAGEMENT)
  public QLSSOProviderConnection fetchConnection(List<QLSSOProviderFilter> appFilters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    List<SSOSettings> ssoSettings = ssoService.getAccountAccessManagementSettings(getAccountId()).getSsoSettings();
    List<QLSSOProvider> nodes = new ArrayList<>();
    int total = 0;
    for (SSOSettings setting : ssoSettings) {
      if (setting.getType() != SSOType.LDAP && setting.getType() != SSOType.SAML) {
        continue;
      }
      QLSSOProviderBuilder builder = QLSSOProvider.builder();
      nodes.add(SSOProviderController.populateSSOProvider(setting, builder).build());
      total += 1;
    }
    QLSSOProviderConnectionBuilder connectionBuilder = QLSSOProviderConnection.builder();
    QLPageInfoBuilder pageInfoBuilder = QLPageInfo.builder();
    QLPageInfo pageInfo = pageInfoBuilder.total(total)
                              .limit(pageQueryParameters.getLimit())
                              .offset(pageQueryParameters.getOffset())
                              .hasMore(total > pageQueryParameters.getOffset())
                              .build();
    connectionBuilder.pageInfo(pageInfo).nodes(nodes);
    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLSSOProviderFilter> filters, Query query) {
    // do nothing
  }

  @Override
  protected QLSSOProviderFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
