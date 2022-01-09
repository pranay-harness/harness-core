/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.anomaly;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.mappers.QlAnomalyMapper;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;

import software.wings.graphql.datafetcher.AbstractAnomalyDataFetcher;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList.QLAnomalyDataListBuilder;
import software.wings.graphql.schema.type.aggregation.billing.QLBillingDataFilter;
import software.wings.graphql.schema.type.aggregation.billing.QLCCMGroupBy;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class K8sAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<QLBillingDataFilter, QLCCMGroupBy> {
  @Inject @Autowired private AnomalyService anomalyService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLAnomalyDataList fetch(String accountId, List<QLBillingDataFilter> filters, List<QLCCMGroupBy> groupBy) {
    QLAnomalyDataListBuilder qlAnomaliesList = QLAnomalyDataList.builder();
    List<AnomalyEntity> anomaliesEntityList = anomalyService.listK8s(accountId, filters, groupBy);
    qlAnomaliesList.data(anomaliesEntityList.stream().map(QlAnomalyMapper::toDto).collect(Collectors.toList()));
    return qlAnomaliesList.build();
  }
}
