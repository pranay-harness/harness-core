package software.wings.service.impl.aws.delegate;

import static io.harness.exception.WingsException.USER;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScaling;
import com.amazonaws.services.applicationautoscaling.AWSApplicationAutoScalingClientBuilder;
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.aws.delegate.AwsAppAutoScalingHelperServiceDelegate;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Singleton
public class AwsAppAutoScalingHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsAppAutoScalingHelperServiceDelegate {
  @VisibleForTesting
  AWSApplicationAutoScaling getAWSApplicationAutoScalingClient(String region, String accessKey, char[] secretKey) {
    return AWSApplicationAutoScalingClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  public RegisterScalableTargetResult registerScalableTarget(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, RegisterScalableTargetRequest scalableTargetRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .registerScalableTarget(scalableTargetRequest);
  }

  public DeregisterScalableTargetResult deregisterScalableTarget(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeregisterScalableTargetRequest deregisterTargetRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .deregisterScalableTarget(deregisterTargetRequest);
  }

  public DescribeScalableTargetsResult listScalableTargets(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeScalableTargetsRequest request) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .describeScalableTargets(request);
  }

  public DescribeScalingPoliciesResult listScalingPolicies(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DescribeScalingPoliciesRequest request) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .describeScalingPolicies(request);
  }

  public PutScalingPolicyResult upsertScalingPolicy(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, PutScalingPolicyRequest putScalingPolicyRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .putScalingPolicy(putScalingPolicyRequest);
  }

  public DeleteScalingPolicyResult deleteScalingPolicy(String region, AwsConfig awsConfig,
      List<EncryptedDataDetail> encryptionDetails, DeleteScalingPolicyRequest deleteScalingPolicyRequest) {
    encryptionService.decrypt(awsConfig, encryptionDetails);
    return getAWSApplicationAutoScalingClient(region, awsConfig.getAccessKey(), awsConfig.getSecretKey())
        .deleteScalingPolicy(deleteScalingPolicyRequest);
  }

  public List<ScalingPolicy> getScalingPolicyFromJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    try {
      if (json.trim().charAt(0) == '[') {
        return mapper.readValue(json, new TypeReference<List<ScalingPolicy>>() {});
      } else {
        return Arrays.asList(mapper.readValue(json, ScalingPolicy.class));
      }
    } catch (IOException e) {
      String errorMsg = "Failed to Deserialize json into AWS Service object" + e;
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  public ScalableTarget getScalableTargetFromJson(String json) {
    ObjectMapper mapper = new ObjectMapper();
    mapper.configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true);
    try {
      return mapper.readValue(json, ScalableTarget.class);
    } catch (IOException e) {
      String errorMsg = "Failed to Deserialize json into AWS Service object" + e;
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  public String getJsonForAwsScalableTarget(ScalableTarget scalableTarget) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(scalableTarget);
    } catch (JsonProcessingException e) {
      String errorMsg = "Failed to Serialize AWS ScalableTarget object into json";
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }

  public String getJsonForAwsScalablePolicy(ScalingPolicy scalingPolicy) {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(scalingPolicy);
    } catch (JsonProcessingException e) {
      String errorMsg = "Failed to Serialize AWS ScalableTarget object into json";
      throw new WingsException(ErrorCode.GENERAL_ERROR, errorMsg, USER).addParam("message", errorMsg);
    }
  }
}