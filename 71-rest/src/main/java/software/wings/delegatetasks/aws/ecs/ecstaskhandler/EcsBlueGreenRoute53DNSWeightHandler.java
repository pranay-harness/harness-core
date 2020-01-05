package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.FAILURE;
import static io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus.SUCCESS;
import static java.lang.String.format;
import static software.wings.beans.Log.LogLevel.ERROR;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.ExceptionUtils;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.helpers.ext.ecs.request.EcsBGRoute53DNSWeightUpdateRequest;
import software.wings.helpers.ext.ecs.request.EcsCommandRequest;
import software.wings.helpers.ext.ecs.response.EcsBGRoute53DNSWeightUpdateResponse;
import software.wings.helpers.ext.ecs.response.EcsCommandExecutionResponse;
import software.wings.service.intfc.aws.delegate.AwsRoute53HelperServiceDelegate;
import software.wings.service.intfc.aws.delegate.AwsServiceDiscoveryHelperServiceDelegate;

import java.util.List;

@Singleton
public class EcsBlueGreenRoute53DNSWeightHandler extends EcsCommandTaskHandler {
  @Inject private EcsContainerService ecsContainerService;
  @Inject private EcsSwapRoutesCommandTaskHelper ecsSwapRoutesCommandTaskHelper;
  @Inject private AwsRoute53HelperServiceDelegate awsRoute53HelperServiceDelegate;
  @Inject private AwsServiceDiscoveryHelperServiceDelegate awsServiceDiscoveryHelperServiceDelegate;

  @Override
  public EcsCommandExecutionResponse executeTaskInternal(EcsCommandRequest ecsCommandRequest,
      List<EncryptedDataDetail> encryptedDataDetails, ExecutionLogCallback executionLogCallback) {
    try {
      if (!(ecsCommandRequest instanceof EcsBGRoute53DNSWeightUpdateRequest)) {
        return EcsCommandExecutionResponse.builder()
            .commandExecutionStatus(FAILURE)
            .ecsCommandResponse(EcsBGRoute53DNSWeightUpdateResponse.builder()
                                    .output("Invalid Request Type: Expected was : [EcsBGRoute53DNSWeightUpdateRequest]")
                                    .commandExecutionStatus(FAILURE)
                                    .build())
            .build();
      }

      EcsBGRoute53DNSWeightUpdateRequest request = (EcsBGRoute53DNSWeightUpdateRequest) ecsCommandRequest;
      int blueServiceWeight;
      String blueServiceValue;
      int greenServiceWeight;
      String greenServiceValue;
      String newServiceValue = awsServiceDiscoveryHelperServiceDelegate.getRecordValueForService(request.getAwsConfig(),
          encryptedDataDetails, request.getRegion(),
          request.getNewServiceDiscoveryArn().substring(request.getNewServiceDiscoveryArn().lastIndexOf('/') + 1));
      String oldServiceValue = awsServiceDiscoveryHelperServiceDelegate.getRecordValueForService(request.getAwsConfig(),
          encryptedDataDetails, request.getRegion(),
          request.getOldServiceDiscoveryArn().substring(request.getOldServiceDiscoveryArn().lastIndexOf('/') + 1));

      if (request.isRollback()) {
        executionLogCallback.saveExecutionLog(format("Upsizing service: [%s] to older count: [%d] in rollback.",
            request.getServiceNameDownsized(), request.getServiceCountDownsized()));
        ecsSwapRoutesCommandTaskHelper.upsizeOlderService(request.getAwsConfig(), encryptedDataDetails,
            request.getRegion(), request.getCluster(), request.getServiceCountDownsized(),
            request.getServiceNameDownsized(), executionLogCallback, request.getTimeout());
        blueServiceWeight = 100;
        blueServiceValue = oldServiceValue;
        greenServiceWeight = 0;
        greenServiceValue = newServiceValue;
      } else {
        blueServiceWeight = request.getNewServiceWeight();
        blueServiceValue = newServiceValue;
        greenServiceWeight = request.getOldServiceWeight();
        greenServiceValue = oldServiceValue;
      }

      executionLogCallback.saveExecutionLog(
          format("Upserting parent record: [%s] with CNAME records: [%s:%d] and [%s:%d]", request.getParentRecordName(),
              blueServiceValue, blueServiceWeight, greenServiceValue, greenServiceWeight));

      awsRoute53HelperServiceDelegate.upsertRoute53ParentRecord(request.getAwsConfig(), encryptedDataDetails,
          request.getRegion(), request.getParentRecordName(), request.getParentRecordHostedZoneId(), blueServiceWeight,
          blueServiceValue, greenServiceWeight, greenServiceValue, request.getTtl());

      executionLogCallback.saveExecutionLog("Swapping ECS tags Blue and Green");
      ecsSwapRoutesCommandTaskHelper.updateServiceTags(request.getAwsConfig(), encryptedDataDetails,
          request.getRegion(), request.getCluster(), request.getServiceName(), request.getServiceNameDownsized(),
          request.isRollback(), executionLogCallback);

      if (!request.isRollback() && request.isDownsizeOldService() && request.getOldServiceWeight() == 0) {
        executionLogCallback.saveExecutionLog("Downsizing old service if needed");
        ecsSwapRoutesCommandTaskHelper.downsizeOlderService(request.getAwsConfig(), encryptedDataDetails,
            request.getRegion(), request.getCluster(), request.getServiceNameDownsized(), executionLogCallback);
      }

      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(SUCCESS)
          .ecsCommandResponse(EcsBGRoute53DNSWeightUpdateResponse.builder().commandExecutionStatus(SUCCESS).build())
          .build();
    } catch (Exception ex) {
      String errorMessage = ExceptionUtils.getMessage(ex);
      executionLogCallback.saveExecutionLog(errorMessage, ERROR);
      return EcsCommandExecutionResponse.builder()
          .commandExecutionStatus(FAILURE)
          .errorMessage(errorMessage)
          .ecsCommandResponse(EcsBGRoute53DNSWeightUpdateResponse.builder().commandExecutionStatus(FAILURE).build())
          .build();
    }
  }
}