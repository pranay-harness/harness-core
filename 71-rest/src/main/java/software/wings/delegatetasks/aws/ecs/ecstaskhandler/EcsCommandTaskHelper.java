package software.wings.delegatetasks.aws.ecs.ecstaskhandler;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;

import com.google.inject.Singleton;

import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ScalingPolicy;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;

import java.util.List;

@Singleton
public class EcsCommandTaskHelper {
  public void registerScalableTargetForEcsService(AwsAppAutoScalingHelperServiceDelegate appAutoScalingService,
      String region, AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      ExecutionLogCallback executionLogCallback, ScalableTarget scalableTarget) {
    if (scalableTarget == null) {
      return;
    }

    executionLogCallback.saveExecutionLog("Registering Scalable Target : " + scalableTarget.getResourceId());
    RegisterScalableTargetRequest request = new RegisterScalableTargetRequest()
                                                .withResourceId(scalableTarget.getResourceId())
                                                .withServiceNamespace(scalableTarget.getServiceNamespace())
                                                .withScalableDimension(scalableTarget.getScalableDimension())
                                                .withRoleARN(scalableTarget.getRoleARN())
                                                .withMinCapacity(scalableTarget.getMinCapacity())
                                                .withMaxCapacity(scalableTarget.getMaxCapacity());

    appAutoScalingService.registerScalableTarget(region, awsConfig, encryptionDetails, request);
    executionLogCallback.saveExecutionLog(new StringBuilder("Registered scalable target Successfully\n")
                                              .append(scalableTarget.toString())
                                              .append("\n")
                                              .toString());
  }

  public void upsertScalingPolicyIfRequired(String policyJson, String resourceId, String scalableDimention,
      String region, AwsConfig awsConfig, AwsAppAutoScalingHelperServiceDelegate appAutoScalingService,
      List<EncryptedDataDetail> encryptionDetails, ExecutionLogCallback executionLogCallback) {
    if (isBlank(policyJson) || isBlank(resourceId)) {
      return;
    }

    List<ScalingPolicy> scalingPolicies = appAutoScalingService.getScalingPolicyFromJson(policyJson);
    if (isNotEmpty(scalingPolicies)) {
      scalingPolicies.forEach(scalingPolicy -> {
        scalingPolicy.withResourceId(resourceId).withScalableDimension(scalableDimention);
        upsertScalingPolicyIfRequired(
            region, awsConfig, appAutoScalingService, encryptionDetails, executionLogCallback, scalingPolicy);
      });
    }
  }

  public void upsertScalingPolicyIfRequired(String region, AwsConfig awsConfig,
      AwsAppAutoScalingHelperServiceDelegate appAutoScalingService, List<EncryptedDataDetail> encryptionDetails,
      ExecutionLogCallback executionLogCallback, ScalingPolicy scalingPolicy) {
    PutScalingPolicyRequest request =
        new PutScalingPolicyRequest()
            .withResourceId(scalingPolicy.getResourceId())
            .withScalableDimension(scalingPolicy.getScalableDimension())
            .withServiceNamespace(scalingPolicy.getServiceNamespace())
            .withPolicyName(scalingPolicy.getPolicyName())
            .withPolicyType(scalingPolicy.getPolicyType())
            .withTargetTrackingScalingPolicyConfiguration(scalingPolicy.getTargetTrackingScalingPolicyConfiguration())
            .withStepScalingPolicyConfiguration(scalingPolicy.getStepScalingPolicyConfiguration());

    executionLogCallback.saveExecutionLog(
        new StringBuilder("Creating Scaling Policy: ").append(scalingPolicy.getPolicyName()).toString());
    PutScalingPolicyResult result =
        appAutoScalingService.upsertScalingPolicy(region, awsConfig, encryptionDetails, request);
    executionLogCallback.saveExecutionLog(
        new StringBuilder("Created Scaling Policy Successfully.\n").append(result.toString()).append("\n").toString());
  }

  public String getResourceIdForEcsService(String serviceName, String clusterName) {
    return new StringBuilder("service/").append(clusterName).append("/").append(serviceName).toString();
  }
}
