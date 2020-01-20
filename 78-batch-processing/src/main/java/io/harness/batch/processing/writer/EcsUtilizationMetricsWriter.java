package io.harness.batch.processing.writer;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.ECS_CLUSTER;
import static io.harness.batch.processing.ccm.UtilizationInstanceType.ECS_SERVICE;

import com.google.inject.Singleton;
import com.google.protobuf.Timestamp;

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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class EcsUtilizationMetricsWriter extends EventWriter implements ItemWriter<PublishedMessage> {
  @Autowired private UtilizationDataServiceImpl utilizationDataService;

  @Override
  public void write(List<? extends PublishedMessage> publishedMessages) {
    logger.info("Published batch size is EcsUtilizationMetricsWriter {} ", publishedMessages.size());
    List<InstanceUtilizationData> instanceUtilizationDataList = new ArrayList<>();

    publishedMessages.stream()
        .filter(publishedMessage -> publishedMessage.getType().equals(EventTypeConstants.ECS_UTILIZATION))
        .forEach(publishedMessage -> {
          String accountId = publishedMessage.getAccountId();
          EcsUtilization ecsUtilization = (EcsUtilization) publishedMessage.getMessage();
          logger.debug("Ecs Utilization {} ", ecsUtilization);

          String serviceArn = ecsUtilization.getServiceArn();
          String serviceName = ecsUtilization.getServiceName();
          String clusterId = ecsUtilization.getClusterId();
          String instanceId;
          String instanceType;
          if (serviceArn.equals("") && serviceName.equals("")) {
            instanceId = ecsUtilization.getClusterArn();
            instanceType = ECS_CLUSTER;
          } else {
            instanceId = serviceArn;
            instanceType = ECS_SERVICE;
          }

          // Initialising List of Metrics to handle Utilization Metrics Downtime (Ideally this will be of size 1)
          // We do not need a Default value as such a scenario will never exist, if there is no data. It will not be
          // inserted to DB.
          List<Double> cpuUtilizationAvgList = new ArrayList<>();
          List<Double> cpuUtilizationMaxList = new ArrayList<>();
          List<Double> memoryUtilizationAvgList = new ArrayList<>();
          List<Double> memoryUtilizationMaxList = new ArrayList<>();
          List<Timestamp> startTimestampList = new ArrayList<>();
          int metricsListSize = 0;

          for (MetricValue utilizationMetric : ecsUtilization.getMetricValuesList()) {
            // Assumption that size of all the metrics and timestamps will be same across the 4 metrics
            startTimestampList = utilizationMetric.getTimestampsList();
            List<Double> metricsList = utilizationMetric.getValuesList();
            metricsListSize = metricsList.size();

            switch (utilizationMetric.getStatistic()) {
              case "Maximum":
                switch (utilizationMetric.getMetricName()) {
                  case "MemoryUtilization":
                    memoryUtilizationMaxList = metricsList;
                    break;
                  case "CPUUtilization":
                    cpuUtilizationMaxList = metricsList;
                    break;
                  default:
                    throw new InvalidRequestException("Invalid Utilization metric name");
                }
                break;
              case "Average":
                switch (utilizationMetric.getMetricName()) {
                  case "MemoryUtilization":
                    memoryUtilizationAvgList = metricsList;
                    break;
                  case "CPUUtilization":
                    cpuUtilizationAvgList = metricsList;
                    break;
                  default:
                    throw new InvalidRequestException("Invalid Utilization metric name");
                }
                break;
              default:
                throw new InvalidRequestException("Invalid Utilization metric Statistic");
            }
          }

          // POJO and insertion to DB
          for (int metricIndex = 0; metricIndex < metricsListSize; metricIndex++) {
            long startTime = startTimestampList.get(metricIndex).getSeconds() * 1000;
            long oneHourMillis = Duration.ofHours(1).toMillis();

            InstanceUtilizationData utilizationData =
                InstanceUtilizationData.builder()
                    .accountId(accountId)
                    .instanceId(instanceId)
                    .instanceType(instanceType)
                    .clusterId(clusterId)
                    .cpuUtilizationMax(getScaledUtilValue(cpuUtilizationMaxList.get(metricIndex)))
                    .cpuUtilizationAvg(getScaledUtilValue(cpuUtilizationAvgList.get(metricIndex)))
                    .memoryUtilizationMax(getScaledUtilValue(memoryUtilizationMaxList.get(metricIndex)))
                    .memoryUtilizationAvg(getScaledUtilValue(memoryUtilizationAvgList.get(metricIndex)))
                    .startTimestamp(startTime)
                    .endTimestamp(startTime + oneHourMillis)
                    .build();

            instanceUtilizationDataList.add(utilizationData);
          }
        });
    utilizationDataService.create(instanceUtilizationDataList);
  }

  private double getScaledUtilValue(double value) {
    return value / 100;
  }
}
