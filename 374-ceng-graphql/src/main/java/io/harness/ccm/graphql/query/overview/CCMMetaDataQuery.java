/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
