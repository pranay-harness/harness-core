package software.wings.service.intfc.aws.delegate;

import com.amazonaws.services.applicationautoscaling.model.DeleteScalingPolicyRequest;
import com.amazonaws.services.applicationautoscaling.model.DeleteScalingPolicyResult;
import com.amazonaws.services.applicationautoscaling.model.DeregisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.DeregisterScalableTargetResult;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalableTargetsResult;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesRequest;
import com.amazonaws.services.applicationautoscaling.model.DescribeScalingPoliciesResult;
import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyRequest;
import com.amazonaws.services.applicationautoscaling.model.PutScalingPolicyResult;
import com.amazonaws.services.applicationautoscaling.model.RegisterScalableTargetRequest;
import com.amazonaws.services.applicationautoscaling.model.RegisterScalableTargetResult;
import com.amazonaws.services.applicationautoscaling.model.ScalableTarget;
import com.amazonaws.services.applicationautoscaling.model.ScalingPolicy;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

public interface AwsAppAutoScalingHelperServiceDelegate {
  RegisterScalableTargetResult registerScalableTarget(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, RegisterScalableTargetRequest scalableTargetRequest);

  DeregisterScalableTargetResult deregisterScalableTarget(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeregisterScalableTargetRequest deregisterTargetRequest);

  DescribeScalableTargetsResult listScalableTargets(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeScalableTargetsRequest request);

  DescribeScalingPoliciesResult listScalingPolicies(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeScalingPoliciesRequest request);

  PutScalingPolicyResult upsertScalingPolicy(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, PutScalingPolicyRequest putScalingPolicyRequest);

  DeleteScalingPolicyResult deleteScalingPolicy(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeleteScalingPolicyRequest deleteScalingPolicyRequest);

  List<ScalingPolicy> getScalingPolicyFromJson(String json);

  ScalableTarget getScalableTargetFromJson(String json);

  String getJsonForAwsScalableTarget(ScalableTarget scalableTarget);

  String getJsonForAwsScalablePolicy(ScalingPolicy scalingPolicy);
}