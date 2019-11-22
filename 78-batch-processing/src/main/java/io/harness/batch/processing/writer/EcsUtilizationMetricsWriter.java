package io.harness.batch.processing.writer;

import com.google.inject.Singleton;

import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.writer.constants.EventTypeConstants;
import io.harness.event.grpc.PublishedMessage;
import io.harness.event.payloads.EcsUtilization;
import io.harness.event.payloads.EcsUtilization.MetricValue;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;

@Slf4j
@Singleton
public class EcsUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private UtilizationDataServiceImpl utilizationDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) {
    logger.info("Published batch size is EcsUtilizationMetricsWriter {} ", publishedMessages.size());
    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_UTILIZATION))
        .forEach(publishedMessage -> {
          EcsUtilization ecsUtilization = (EcsUtilization) publishedMessage.getMessage();
          logger.info("Ecs Utilization {} ", ecsUtilization);

          // Handle the Utilization event
          double cpuUtilizationAvg = 0.0;
          double cpuUtilizationMax = 0.0;
          double memoryUtilizationAvg = 0.0;
          double memoryUtilizationMax = 0.0;

          long endTimestamp = 0L;
          long startTimestamp = 0L;

          for (MetricValue utilizationMetric : ecsUtilization.getMetricValuesList()) {
            if (!utilizationMetric.getTimestampsList().isEmpty()) {
              startTimestamp = utilizationMetric.getTimestampsList().get(0).getSeconds() * 1000;
              endTimestamp = utilizationMetric.getTimestampsList()
                                 .get(utilizationMetric.getTimestampsList().size() - 1)
                                 .getSeconds()
                  * 1000;
            }
            switch (utilizationMetric.getStatistic()) {
              case "Maximum":
                switch (utilizationMetric.getMetricName()) {
                  case "MemoryUtilization":
                    memoryUtilizationMax = Collections.max(utilizationMetric.getValuesList());
                    break;
                  case "CPUUtilization":
                    cpuUtilizationMax = Collections.max(utilizationMetric.getValuesList());
                    break;
                  default:
                    throw new InvalidRequestException("Invalid Utilization metric name");
                }
                break;
              case "Average":
                switch (utilizationMetric.getMetricName()) {
                  case "MemoryUtilization":
                    memoryUtilizationAvg =
                        utilizationMetric.getValuesList().stream().mapToDouble(val -> val).average().orElse(0.0);
                    break;
                  case "CPUUtilization":
                    cpuUtilizationAvg =
                        utilizationMetric.getValuesList().stream().mapToDouble(val -> val).average().orElse(0.0);
                    break;
                  default:
                    throw new InvalidRequestException("Invalid Utilization metric name");
                }
                break;
              default:
                throw new InvalidRequestException("Invalid Utilization metric Statistic");
            }
          }

          InstanceUtilizationData utilizationData = InstanceUtilizationData.builder()
                                                        .clusterArn(ecsUtilization.getClusterArn())
                                                        .clusterName(ecsUtilization.getClusterName())
                                                        .serviceArn(ecsUtilization.getServiceArn())
                                                        .serviceName(ecsUtilization.getServiceName())
                                                        .cpuUtilizationMax(cpuUtilizationMax)
                                                        .cpuUtilizationAvg(cpuUtilizationAvg)
                                                        .memoryUtilizationMax(memoryUtilizationMax)
                                                        .memoryUtilizationAvg(memoryUtilizationAvg)
                                                        .startTimestamp(startTimestamp)
                                                        .endTimestamp(endTimestamp)
                                                        .build();
          utilizationDataService.create(utilizationData);
        });
  }
}
