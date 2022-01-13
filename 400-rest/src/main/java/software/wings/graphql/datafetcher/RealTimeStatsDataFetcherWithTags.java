/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.QLData;
import software.wings.graphql.schema.type.aggregation.TagAggregation;

import java.util.List;

@OwnedBy(DX)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public abstract class RealTimeStatsDataFetcherWithTags<A, F, G, S, E, TA extends TagAggregation, EA>
    extends AbstractStatsDataFetcherWithTags<A, F, G, S, E, TA, EA> implements BaseRealTimeStatsDataFetcher<F> {
  protected QLData getQLData(String accountId, List<F> filters, Class entityClass, List<String> groupByAsStringList) {
    return getQLData(utils, nameService, wingsPersistence, accountId, filters, entityClass, groupByAsStringList);
  }
}
