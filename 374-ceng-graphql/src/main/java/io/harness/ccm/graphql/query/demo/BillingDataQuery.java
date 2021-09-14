/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.query.demo;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.graphql.dto.demo.BillingDataDemo;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class BillingDataQuery {
  @Inject private GraphQLUtils graphQLUtils;
  // GraphQL Query Schema and Service Class
  @GraphQLQuery
  public List<BillingDataDemo> billingdata(
      String clusterid, OffsetTime startTime, OffsetTime endTime, @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    return getBillingData(accountId, clusterid);
  }

  // DAO
  private List<BillingDataDemo> getBillingData(String accountId, String clusterid) {
    // generate fake data
    List<BillingDataDemo> billingDataDemoList = new ArrayList<>();
    billingDataDemoList.add(BillingDataDemo.of("id1", "name1", 101.0, graphQLUtils.currentTime()));
    billingDataDemoList.add(BillingDataDemo.of("id2", "name2", 102.0, graphQLUtils.currentTime()));
    billingDataDemoList.add(BillingDataDemo.of("id3", "name3", 103.0, graphQLUtils.currentTime()));
    billingDataDemoList.add(BillingDataDemo.of("id4", "name4", 104.0, graphQLUtils.currentTime()));
    return billingDataDemoList;
  }
}
