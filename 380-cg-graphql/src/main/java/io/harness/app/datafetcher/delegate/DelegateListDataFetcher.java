package io.harness.app.datafetcher.delegate;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.app.schema.query.delegate.QLDelegateFilter;
import io.harness.app.schema.type.delegate.QLDelegate;
import io.harness.app.schema.type.delegate.QLDelegateList;
import io.harness.app.schema.type.delegate.QLDelegateStatus;
import io.harness.delegate.beans.Delegate;

import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;

public class DelegateListDataFetcher
    extends AbstractConnectionV2DataFetcher<io.harness.app.schema.query.delegate.QLDelegateFilter, QLNoOpSortCriteria,
        QLDelegateList> {
  @Override
  protected io.harness.app.schema.query.delegate.QLDelegateFilter generateFilter(
      DataFetchingEnvironment environment, String key, String value) {
    return null;
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLDelegateList fetchConnection(List<io.harness.app.schema.query.delegate.QLDelegateFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<Delegate> query = populateFilters(wingsPersistence, filters, Delegate.class, true)
                                .order(Sort.descending(Delegate.DelegateKeys.createdAt));

    QLDelegateList.QLDelegateListBuilder delegateListBuilder = QLDelegateList.builder();
    delegateListBuilder.pageInfo(utils.populate(pageQueryParameters, query, delegate -> {
      QLDelegate.QLDelegateBuilder builder = QLDelegate.builder();
      DelegateController.populateQLDelegate(delegate, builder);
      delegateListBuilder.node(builder.build());
    }));
    return delegateListBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLDelegateFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }
    filters.forEach(filter -> {
      FieldEnd<? extends Query<Delegate>> field;
      if (filter.getDelegateName() != null) {
        field = query.field("delegateName");
        String name = filter.getDelegateName();
        utils.setStringFilter(field, name);
      }
      if (filter.getDelegateStatus() != null) {
        field = query.field("status");
        QLDelegateStatus filterDelegateStatus = filter.getDelegateStatus();
        // utils.setEnumFilter(field,filterDelegateStatus);
      }
      if (filter.getDelegateType() != null) {
        field = query.field("delegateType");
        String type = filter.getDelegateType().getStringValue();
        utils.setStringFilter(field, type);
      }
    });
  }
}
