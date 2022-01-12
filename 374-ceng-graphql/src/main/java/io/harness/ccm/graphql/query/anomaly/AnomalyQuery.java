/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.query.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.anomaly.AnomalyDataList;
import io.harness.ccm.graphql.core.anomaly.AnomalyService;
import io.harness.ccm.graphql.utils.GraphQLUtils;
import io.harness.ccm.graphql.utils.annotations.GraphQLApi;
import io.harness.ccm.views.graphql.QLCEViewFilterWrapper;
import io.harness.ccm.views.graphql.QLCEViewGroupBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.leangen.graphql.annotations.GraphQLArgument;
import io.leangen.graphql.annotations.GraphQLEnvironment;
import io.leangen.graphql.annotations.GraphQLQuery;
import io.leangen.graphql.execution.ResolutionEnvironment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@GraphQLApi
@OwnedBy(CE)
public class AnomalyQuery {
  @Inject private GraphQLUtils graphQLUtils;
  @Inject private AnomalyService anomalyService;

  @GraphQLQuery(name = "anomaliesForPerspective", description = "Get Anomalies for perspective")
  public AnomalyDataList anomaliesForPerspective(@GraphQLArgument(name = "filters") List<QLCEViewFilterWrapper> filters,
      @GraphQLArgument(name = "groupBy") List<QLCEViewGroupBy> groupBy,
      @GraphQLEnvironment final ResolutionEnvironment env) {
    final String accountId = graphQLUtils.getAccountIdentifier(env);
    return anomalyService.getAnomalyDataForPerspective(filters, groupBy, accountId);
  }
}
