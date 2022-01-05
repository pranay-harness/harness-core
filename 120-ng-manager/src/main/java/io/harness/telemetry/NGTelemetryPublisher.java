package io.harness.telemetry;

import io.harness.cdng.pipeline.helpers.CDPipelineInstrumentationHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.dtos.InstanceDTO;
import io.harness.service.instance.InstanceService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.query.Criteria;

@Slf4j
@Singleton
public class NGTelemetryPublisher {
  @Inject TelemetryReporter telemetryReporter;
  @Inject CDPipelineInstrumentationHelper cdPipelineInstrumentationHelper;
  @Inject InstanceService instanceService;

  String TOTAL_DISTINCT_ACTIVE_SERVICES_IN_A_DAY = "total_distinct_active_services_in_a_day";
  String TOTAL_DISTINCT_ACTIVE_SERVICES_IN_A_MONTH = "total_distinct_active_services_in_a_month";
  String TOTAL_NUMBER_OF_SERVICE_INSTANCES_IN_A_DAY = "total_number_of_service_instances_in_a_day";
  String TOTAL_NUMBER_OF_SERVICE_INSTANCES_IN_A_MONTH = "total_number_of_service_instances_in_a_month";

  public void recordTelemetry() {
    log.info("NGTelemetryPublisher recordTelemetry execute started.");
    try {
      Long CURRENT_TIMESTAMP = System.currentTimeMillis();
      Long MILLISECONDS_IN_A_DAY = 86400000L;
      Long MILLISECONDS_IN_A_MONTH = 2592000000L;
      Long totalDistinctActiveServicesInADay = 0L;
      Long totalDistinctActiveServicesInAMonth = 0L;
      Long totalNumberOfServiceInstancesInADay = 0L;
      Long totalNumberOfServiceInstancesInAMonth = 0L;

      String accountId = getAccountId();

      if (EmptyPredicate.isNotEmpty(accountId)) {
        List<InstanceDTO> serviceInstancesInAMonth = cdPipelineInstrumentationHelper.getServiceInstancesInInterval(
            accountId, CURRENT_TIMESTAMP - MILLISECONDS_IN_A_MONTH, CURRENT_TIMESTAMP);
        totalNumberOfServiceInstancesInAMonth = (long) serviceInstancesInAMonth.size();
        totalDistinctActiveServicesInAMonth =
            cdPipelineInstrumentationHelper.getTotalNumberOfActiveServices(serviceInstancesInAMonth);

        List<InstanceDTO> serviceInstancesInADay = cdPipelineInstrumentationHelper.getServiceInstancesInInterval(
            accountId, CURRENT_TIMESTAMP - MILLISECONDS_IN_A_DAY, CURRENT_TIMESTAMP);
        totalNumberOfServiceInstancesInADay = (long) serviceInstancesInADay.size();
        totalDistinctActiveServicesInADay =
            cdPipelineInstrumentationHelper.getTotalNumberOfActiveServices(serviceInstancesInADay);

        HashMap<String, Object> map = new HashMap<>();
        map.put(TOTAL_DISTINCT_ACTIVE_SERVICES_IN_A_DAY, totalDistinctActiveServicesInADay);
        map.put(TOTAL_DISTINCT_ACTIVE_SERVICES_IN_A_MONTH, totalDistinctActiveServicesInAMonth);
        map.put(TOTAL_NUMBER_OF_SERVICE_INSTANCES_IN_A_DAY, totalNumberOfServiceInstancesInADay);
        map.put(TOTAL_NUMBER_OF_SERVICE_INSTANCES_IN_A_MONTH, totalNumberOfServiceInstancesInAMonth);
        telemetryReporter.sendGroupEvent(
            accountId, null, map, null, TelemetryOption.builder().sendForCommunity(true).build());
        log.info("Scheduled NGTelemetryPublisher event sent!");
      } else {
        log.info("There is no Account found!. Can not send scheduled NGTelemetryPublisher event.");
      }
    } catch (Exception e) {
      log.error("NGTelemetryPublisher recordTelemetry execute failed.", e);
    } finally {
      log.info("NGTelemetryPublisher recordTelemetry execute finished.");
    }
  }

  private String getAccountId() {
    Criteria criteria = new Criteria();
    InstanceDTO instanceDTO = instanceService.findFirstInstance(criteria);
    if (instanceDTO != null) {
      return instanceDTO.getAccountIdentifier();
    }
    return null;
  }
}
