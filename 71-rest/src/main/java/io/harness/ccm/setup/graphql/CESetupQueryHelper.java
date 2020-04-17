package io.harness.ccm.setup.graphql;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;

import java.util.List;

@Singleton
public class CESetupQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLCESetupFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getCloudProviderId() != null) {
        field = query.field("cloudProviderId");
        QLIdFilter cloudProviderIdFilter = filter.getCloudProviderId();
        utils.setIdFilter(field, cloudProviderIdFilter);
      }

      if (filter.getInfraMasterAccountId() != null) {
        field = query.field("infraMasterAccountId");
        QLIdFilter infraMasterAccountIdFilter = filter.getInfraMasterAccountId();
        utils.setIdFilter(field, infraMasterAccountIdFilter);
      }

      if (filter.getMasterAccountSettingId() != null) {
        field = query.field("masterAccountSettingId");
        QLIdFilter masterAccountSettingIdFilter = filter.getMasterAccountSettingId();
        utils.setIdFilter(field, masterAccountSettingIdFilter);
      }
    });
  }
}
