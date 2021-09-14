/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.graphql.query.overview;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.graphql.core.overview.CCMMetaDataService;
import io.harness.ccm.graphql.dto.overview.CCMMetaData;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class CCMMetaDataQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private CCMMetaDataService ccmMetaDataService;

  @GraphQLQuery(name = "ccmMetaData", description = "Fetch CCM MetaData for account")
  public CCMMetaData ccmMetaData(@GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    return ccmMetaDataService.getCCMMetaData(accountId);
  }
}
