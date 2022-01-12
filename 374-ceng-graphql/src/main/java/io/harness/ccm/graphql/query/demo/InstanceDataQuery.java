/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.query.demo;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.graphql.dto.demo.BillingDataDemo;
import io.harness.ccm.graphql.dto.demo.InstanceDataDemo;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLContext;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLNonNull;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import lombok.Getter;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.dataloader.DataLoader;

/**
 * For documentation/examples refer https://github.com/leangen/graphql-spqr-samples
 */
@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class InstanceDataQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Getter private final String dataLoaderName = "instancedata";

  // GraphQL Query Schema and Service Class
  @GraphQLQuery
  public CompletableFuture<InstanceDataDemo> instancedata(
      @GraphQLContext BillingDataDemo billingDataDemo, @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    final DataLoader<CacheKey, InstanceDataDemo> dataLoader = env.dataFetchingEnvironment.getDataLoader("instancedata");

    log.debug("INSIDE: getInstanceDataById Query In BillingData Context");
    return dataLoader.load(CacheKey.of(accountId, billingDataDemo.getInstanceid()));
  }

  @GraphQLQuery
  public CompletableFuture<InstanceDataDemo> instancedata(
      @GraphQLNonNull String instanceid, @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);

    log.debug("INSIDE: getInstanceDataById Query");
    return instanceDataLoader.load(CacheKey.of(accountId, instanceid));
  }

  @Value(staticConstructor = "of")
  private static class CacheKey {
    String accountId;
    String clusterId;
  }

  // DataLoader and DAO
  private final DataLoader<CacheKey, InstanceDataDemo> instanceDataLoader =
      DataLoader.newDataLoader(instanceIds -> CompletableFuture.supplyAsync(() -> getInstanceDataByIds(instanceIds)));

  private List<InstanceDataDemo> getInstanceDataByIds(final List<CacheKey> keys) {
    log.debug("INSIDE: getInstanceDataByIds DAO");
    List<InstanceDataDemo> result = new ArrayList<>();
    for (CacheKey id : keys) {
      result.add(InstanceDataDemo.builder()
                     .cloudprovider("cloudprovider_" + id.getAccountId() + "/" + id.getClusterId())
                     .instancetype("instance_type_" + id.getAccountId() + "/" + id.getClusterId())
                     .region("region_" + id.getAccountId() + "/" + id.getClusterId())
                     .build());
    }
    return result;
  }
}
