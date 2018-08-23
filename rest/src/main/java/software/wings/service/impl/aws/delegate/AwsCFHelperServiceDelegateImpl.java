package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import java.util.List;

@Singleton
public class AwsCFHelperServiceDelegateImpl extends AwsHelperServiceDelegateBase implements AwsCFHelperServiceDelegate {
  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(Regions region, String accessKey, char[] secretKey) {
    return (AmazonCloudFormationClient) AmazonCloudFormationClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public List<AwsCFTemplateParamsData> getParamsData(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String region, String data, String type) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudFormationClient client =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      if ("s3".equalsIgnoreCase(type)) {
        request.withTemplateURL(data);
      } else {
        request.withTemplateBody(data);
      }
      GetTemplateSummaryResult result = client.getTemplateSummary(request);
      List<ParameterDeclaration> parameters = result.getParameters();
      if (isNotEmpty(parameters)) {
        return parameters.stream()
            .map(parameter
                -> AwsCFTemplateParamsData.builder()
                       .paramKey(parameter.getParameterKey())
                       .paramType(parameter.getParameterType())
                       .defaultValue(parameter.getDefaultValue())
                       .build())
            .collect(toList());
      }
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    }
    return emptyList();
  }

  @Override
  public String getStackBody(AwsConfig awsConfig, String region, String stackId) {
    try {
      AmazonCloudFormationClient client =
          getAmazonCloudFormationClient(Regions.fromName(region), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      GetTemplateRequest getTemplateRequest = new GetTemplateRequest().withStackName(stackId);
      GetTemplateResult getTemplateResult = client.getTemplate(getTemplateRequest);
      return getTemplateResult.getTemplateBody();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    }
    return "";
  }
}
