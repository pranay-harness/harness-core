package io.harness.perpetualtask;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.CollectionUtils.emptyIfNull;
import static io.harness.network.SafeHttpCall.execute;
import static io.harness.perpetualtask.PerpetualTaskState.TASK_RUN_FAILED;
import static io.harness.perpetualtask.PerpetualTaskState.TASK_RUN_SUCCEEDED;
import static java.lang.Boolean.TRUE;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.joda.time.Seconds.secondsBetween;
import static software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey.INVOCATION_COUNT_KEY_LIST;

import com.google.inject.Inject;

import com.amazonaws.services.cloudwatch.model.Datapoint;
import com.amazonaws.services.cloudwatch.model.Dimension;
import io.harness.exception.InvalidArgumentsException;
import io.harness.grpc.utils.AnyUtils;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.perpetualtask.instancesync.AwsLambdaInstanceSyncPerpetualTaskParams;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoUtils;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.jetty.server.Response;
import org.joda.time.DateTime;
import software.wings.beans.AwsConfig;
import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.service.impl.aws.model.request.AwsCloudWatchStatisticsRequest;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsCloudWatchStatisticsResponse;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsMetricsResponse;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.intfc.aws.delegate.AwsCloudWatchHelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class AwsLambdaInstanceSyncPerpetualTaskExecutor implements PerpetualTaskExecutor {
  @Inject private DelegateAgentManagerClient delegateAgentManagerClient;
  @Inject private AwsLambdaHelperServiceDelegate awsLambdaHelperServiceDelegate;
  @Inject private AwsCloudWatchHelperServiceDelegate awsCloudWatchHelperServiceDelegate;

  @Override
  public PerpetualTaskResponse runOnce(
      PerpetualTaskId taskId, PerpetualTaskExecutionParams params, Instant heartbeatTime) {
    logger.info("Running the InstanceSync perpetual task executor for task id: {}", taskId);
    final AwsLambdaInstanceSyncPerpetualTaskParams taskParams =
        AnyUtils.unpack(params.getCustomizedParams(), AwsLambdaInstanceSyncPerpetualTaskParams.class);

    final AwsConfig awsConfig = (AwsConfig) KryoUtils.asObject(taskParams.getAwsConfig().toByteArray());

    AwsLambdaDetailsMetricsResponse awsLambdaDetailsWithMetricsResponse = getAwsResponse(taskParams, awsConfig);
    publishAwsLambdaSyncResult(taskId, taskParams, awsConfig, awsLambdaDetailsWithMetricsResponse);

    return toPerpetualTaskResponse(awsLambdaDetailsWithMetricsResponse);
  }

  @Override
  public boolean cleanup(PerpetualTaskId taskId, PerpetualTaskExecutionParams params) {
    return false;
  }

  private void publishAwsLambdaSyncResult(PerpetualTaskId taskId, AwsLambdaInstanceSyncPerpetualTaskParams taskParams,
      AwsConfig awsConfig, AwsLambdaDetailsMetricsResponse awsLambdaDetailsWithMetricsResponse) {
    try {
      execute(delegateAgentManagerClient.publishInstanceSyncResult(
          taskId.getId(), awsConfig.getAccountId(), awsLambdaDetailsWithMetricsResponse));
    } catch (Exception e) {
      logger.error(
          String.format("Failed to publish instance sync result for aws lambda. Function [%s] and PerpetualTaskId [%s]",
              taskParams.getFunctionName(), taskId.getId()),
          e);
    }
  }

  private AwsLambdaDetailsMetricsResponse getAwsResponse(
      AwsLambdaInstanceSyncPerpetualTaskParams taskParams, AwsConfig awsConfig) {
    @SuppressWarnings("unchecked")
    final List<EncryptedDataDetail> encryptedDataDetails =
        (List<EncryptedDataDetail>) KryoUtils.asObject(taskParams.getEncryptedData().toByteArray());

    AwsLambdaDetailsResponse awsLambdaDetailsResponse =
        getAwsLambdaDetailsResponse(taskParams, awsConfig, encryptedDataDetails);

    if (FAILED == awsLambdaDetailsResponse.getExecutionStatus()) {
      // no need to get the metrics
      return AwsLambdaDetailsMetricsResponse.builder()
          .executionStatus(FAILED)
          .errorMessage(awsLambdaDetailsResponse.getErrorMessage())
          .build();
    }

    return AwsLambdaDetailsMetricsResponse.builder()
        .delegateMetaInfo(awsLambdaDetailsResponse.getDelegateMetaInfo())
        .executionStatus(SUCCESS)
        .lambdaDetails(awsLambdaDetailsResponse.getLambdaDetails())
        .invocationCountList(getInvocationCountList(taskParams, awsConfig, encryptedDataDetails))
        .build();
  }

  private List<InvocationCount> getInvocationCountList(AwsLambdaInstanceSyncPerpetualTaskParams taskParams,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    try {
      return INVOCATION_COUNT_KEY_LIST.stream()
          .map(invocationCountKey -> getInvocationCountParameters(taskParams, invocationCountKey))
          .map(param
              -> Pair.of(param, getCloudWatchStatisticsResponse(taskParams, awsConfig, encryptedDataDetails, param)))
          .filter(paramWithResponse -> SUCCESS == paramWithResponse.getValue().getExecutionStatus())
          .map(paramWithResponse
              -> Pair.of(paramWithResponse.getKey(), getInvocationCountOptional(paramWithResponse.getValue())))
          .filter(paramWithInvocationCount -> paramWithInvocationCount.getValue().isPresent())
          .map(paramWithInvocationCount
              -> Pair.of(paramWithInvocationCount.getKey(), paramWithInvocationCount.getValue().get()))
          .map(paramWithInvocationCountValue
              -> InvocationCount.builder()
                     .key(paramWithInvocationCountValue.getKey().getKey())
                     .count(paramWithInvocationCountValue.getValue().longValue())
                     .from(paramWithInvocationCountValue.getKey().getStartDate().toInstant())
                     .to(paramWithInvocationCountValue.getKey().getEndDate().toInstant())
                     .build())
          .collect(Collectors.toList());
    } catch (Exception exception) {
      logger.error(String.format("Failed to get the invocation count for function: [%s]", taskParams.getFunctionName()),
          exception);
      return emptyList();
    }
  }

  private Optional<Double> getInvocationCountOptional(AwsCloudWatchStatisticsResponse awsCloudWatchStatisticsResponse) {
    return emptyIfNull(awsCloudWatchStatisticsResponse.getDatapoints())
        .stream()
        .map(Datapoint::getSum)
        .reduce(Double::sum);
  }

  private InvocationCountParameters getInvocationCountParameters(
      AwsLambdaInstanceSyncPerpetualTaskParams taskParams, InvocationCountKey invocationCountKey) {
    Date endDate = new Date();
    Date startDate = getStartDate(endDate, new Date(taskParams.getStartDate()), invocationCountKey);
    return InvocationCountParameters.builder()
        .key(invocationCountKey)
        .startDate(startDate)
        .endDate(endDate)
        .period(getPeriod(startDate, endDate))
        .build();
  }

  private AwsCloudWatchStatisticsResponse getCloudWatchStatisticsResponse(
      AwsLambdaInstanceSyncPerpetualTaskParams taskParams, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptedDataDetails, InvocationCountParameters invocationCountParameters) {
    final AwsCloudWatchStatisticsRequest request =
        AwsCloudWatchStatisticsRequest.builder()
            .namespace("AWS/Lambda")
            .dimensions(singletonList(new Dimension().withName("FunctionName").withValue(taskParams.getFunctionName())))
            .metricName("Invocations")
            .statistics(singletonList("Sum"))
            .startTime(invocationCountParameters.getStartDate())
            .endTime(invocationCountParameters.getEndDate())
            .period(invocationCountParameters.getPeriod())
            .region(taskParams.getRegion())
            .awsConfig(awsConfig)
            .encryptionDetails(encryptedDataDetails)
            .build();
    try {
      return awsCloudWatchHelperServiceDelegate.getMetricStatistics(request);
    } catch (Exception ex) {
      logger.error(
          String.format("Failed to execute aws CloudWatch statistics: [%s]", taskParams.getFunctionName()), ex);
      return AwsCloudWatchStatisticsResponse.builder().executionStatus(FAILED).errorMessage(ex.getMessage()).build();
    }
  }

  private Date getStartDate(Date endDate, Date deployedAt, InvocationCountKey invocationCountKey) {
    switch (invocationCountKey) {
      case LAST_30_DAYS:
        return DateUtils.addDays(endDate, -30);
      case SINCE_LAST_DEPLOYED:
        return deployedAt;
      default:
        throw new InvalidArgumentsException(Pair.of("invocationCountKey", invocationCountKey.name()));
    }
  }

  private int getPeriod(Date startDate, Date endDate) {
    final DateTime fromDateTime = new DateTime(startDate);
    final DateTime toDateTime = new DateTime(endDate);
    int seconds = secondsBetween(fromDateTime, toDateTime).getSeconds();

    if (seconds == 0) {
      return 60;
    }

    if (seconds % 60 == 0) {
      return seconds;
    }

    return ((seconds / 60) + 1) * 60;
  }

  private AwsLambdaDetailsResponse getAwsLambdaDetailsResponse(AwsLambdaInstanceSyncPerpetualTaskParams taskParams,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    AwsLambdaDetailsRequest request = AwsLambdaDetailsRequest.builder()
                                          .awsConfig(awsConfig)
                                          .encryptionDetails(encryptedDataDetails)
                                          .region(taskParams.getRegion())
                                          .functionName(taskParams.getFunctionName())
                                          .qualifier(taskParams.getQualifier())
                                          .loadAliases(TRUE)
                                          .build();
    try {
      return awsLambdaHelperServiceDelegate.getFunctionDetails(request);
    } catch (Exception ex) {
      logger.error(String.format("Failed to execute aws function: [%s]", taskParams.getFunctionName()), ex);
      return AwsLambdaDetailsResponse.builder().executionStatus(FAILED).errorMessage(ex.getMessage()).build();
    }
  }

  private PerpetualTaskResponse toPerpetualTaskResponse(AwsLambdaDetailsMetricsResponse response) {
    boolean failed = FAILED == response.getExecutionStatus();
    return PerpetualTaskResponse.builder()
        .responseCode(Response.SC_OK)
        .perpetualTaskState(failed ? TASK_RUN_FAILED : TASK_RUN_SUCCEEDED)
        .responseMessage(failed ? response.getErrorMessage() : TASK_RUN_SUCCEEDED.name())
        .build();
  }

  @Data
  @Builder
  static class InvocationCountParameters {
    InvocationCountKey key;
    Date startDate;
    Date endDate;
    int period;
  }
}
