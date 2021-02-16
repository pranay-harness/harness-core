package software.wings.graphql.datafetcher.anomaly;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.mappers.QlAnomalyMapper;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;
import io.harness.ccm.billing.graphql.CloudBillingFilter;
import io.harness.ccm.billing.graphql.CloudBillingGroupBy;

import software.wings.graphql.datafetcher.AbstractAnomalyDataFetcher;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList;
import software.wings.graphql.schema.type.aggregation.anomaly.QLAnomalyDataList.QLAnomalyDataListBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@TargetModule(Module._380_CG_GRAPHQL)
public class CloudAnomaliesDataFetcher extends AbstractAnomalyDataFetcher<CloudBillingFilter, CloudBillingGroupBy> {
  @Inject @Autowired private AnomalyService anomalyService;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLAnomalyDataList fetch(
      String accountId, List<CloudBillingFilter> filters, List<CloudBillingGroupBy> groupBy) {
    QLAnomalyDataListBuilder qlAnomaliesList = QLAnomalyDataList.builder();
    List<AnomalyEntity> anomaliesEntityList = anomalyService.listCloud(accountId, filters, groupBy);
    qlAnomaliesList.data(anomaliesEntityList.stream().map(QlAnomalyMapper::toDto).collect(Collectors.toList()));
    return qlAnomaliesList.build();
  }
}
