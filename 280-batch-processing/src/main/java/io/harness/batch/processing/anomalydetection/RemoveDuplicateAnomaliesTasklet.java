package io.harness.batch.processing.anomalydetection;

import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.ccm.anomaly.entities.AnomalyEntity;
import io.harness.ccm.anomaly.service.itfc.AnomalyService;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.inject.Inject;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class RemoveDuplicateAnomaliesTasklet implements Tasklet {
  private JobParameters parameters;

  @Autowired @Inject private AnomalyService anomalyService;

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    Instant startTime = getFieldValueFromJobParams(CCMJobConstants.JOB_START_DATE);
    log.info("[RDA] Executing delete step {} {}", accountId, startTime);

    List<AnomalyEntity> anomalyList = anomalyService.list(accountId, startTime);
    log.info("[RDA] anomaly list {}", anomalyList.size());
    List<String> duplicateIds = getDuplicates(anomalyList);
    log.info("[RDA] deleting anomalies {}", duplicateIds);

    if (!duplicateIds.isEmpty()) {
      removeDuplicates(duplicateIds, startTime);
    }
    return null;
  }

  private void removeDuplicates(List<String> ids, Instant startTime) {
    anomalyService.delete(ids, startTime);
  }

  private List<String> getDuplicates(List<AnomalyEntity> anomaliesList) {
    List<AnomalyEntity> sorted = anomaliesList.stream()
                                     .sorted(Comparator.comparing(this::getConcatString).reversed())
                                     .collect(Collectors.toList());
    List<String> idList = new ArrayList<>();

    int currentDepth = -1;

    for (AnomalyEntity anomaly : sorted) {
      if (getDepth(anomaly) < currentDepth) {
        idList.add(anomaly.getId());
      }
      currentDepth = getDepth(anomaly);
    }
    return idList;
  }

  private int getDepth(AnomalyEntity anomaly) {
    List<String> list = new ArrayList<>(Arrays.asList(anomaly.getClusterId(), anomaly.getNamespace(),
        anomaly.getWorkloadName(), anomaly.getGcpProject(), anomaly.getGcpProduct(), anomaly.getGcpSKUId(),
        anomaly.getAwsAccount(), anomaly.getAwsService()));
    Iterables.removeIf(list, Objects::isNull);
    return list.size();
  }

  private String getConcatString(AnomalyEntity anomaly) {
    return Joiner.on(" ").skipNulls().join(anomaly.getClusterId(), anomaly.getNamespace(), anomaly.getWorkloadName(),
        anomaly.getGcpProject(), anomaly.getGcpProduct(), anomaly.getGcpSKUId(), anomaly.getAwsAccount(),
        anomaly.getAwsService());
  }
  private Instant getFieldValueFromJobParams(String fieldName) {
    return Instant.ofEpochMilli(Long.parseLong(parameters.getString(fieldName)));
  }
}
