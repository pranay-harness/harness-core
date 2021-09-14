/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.userGroup;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.security.UserGroup;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.usergroup.QLUserGroupFilter;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;

@Singleton
@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UserGroupQueryHelper {
  @Inject private DataFetcherUtils utils;

  public void setQuery(List<QLUserGroupFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<UserGroup>> field;

      if (filter.getUser() != null) {
        field = query.field("memberIds");
        QLIdFilter userFilter = filter.getUser();
        utils.setIdFilter(field, userFilter);
      }
    });
  }
}
