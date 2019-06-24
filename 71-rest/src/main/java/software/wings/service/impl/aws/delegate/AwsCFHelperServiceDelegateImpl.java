package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.AmazonCloudFormationClientBuilder;
import com.amazonaws.services.cloudformation.model.GetTemplateRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryRequest;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.AwsConfig;
import software.wings.beans.CloudFormationSourceType;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.GitOperationContext;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.impl.yaml.GitClientHelper;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.yaml.GitClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Singleton
public class AwsCFHelperServiceDelegateImpl extends AwsHelperServiceDelegateBase implements AwsCFHelperServiceDelegate {
  private static final String USER_DIR_KEY = "user.dir";

  @Inject private EncryptionService encryptionService;
  @Inject private GitClient gitClient;
  @Inject private GitClientHelper gitClientHelper;

  @VisibleForTesting
  AmazonCloudFormationClient getAmazonCloudFormationClient(
      Regions region, String accessKey, char[] secretKey, boolean useEc2IamCredentials) {
    AmazonCloudFormationClientBuilder builder = AmazonCloudFormationClientBuilder.standard().withRegion(region);
    attachCredentials(builder, useEc2IamCredentials, accessKey, secretKey);
    return (AmazonCloudFormationClient) builder.build();
  }

  @Override
  public List<AwsCFTemplateParamsData> getParamsData(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String region, String data, String type, GitFileConfig gitFileConfig, GitConfig gitConfig,
      List<EncryptedDataDetail> sourceRepoEncryptedDetail) {
    try {
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AmazonCloudFormationClient client = getAmazonCloudFormationClient(Regions.fromName(region),
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      // TODO: Have enum instead of string "s3"
      if ("s3".equalsIgnoreCase(type)) {
        request.withTemplateURL(data);
      } else if (CloudFormationSourceType.GIT.name().equalsIgnoreCase(type)) {
        GitOperationContext gitOperationContext = cloneRepo(gitConfig, gitFileConfig, sourceRepoEncryptedDetail);
        String absoluteTemplatePath = resolveScriptDirectory(gitOperationContext, gitFileConfig.getFilePath());
        request.withTemplateBody(getRequestDataFromFile(absoluteTemplatePath));
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
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  @Override
  public String getStackBody(AwsConfig awsConfig, String region, String stackId) {
    try {
      AmazonCloudFormationClient client = getAmazonCloudFormationClient(Regions.fromName(region),
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      GetTemplateRequest getTemplateRequest = new GetTemplateRequest().withStackName(stackId);
      GetTemplateResult getTemplateResult = client.getTemplate(getTemplateRequest);
      return getTemplateResult.getTemplateBody();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return "";
  }

  @Override
  public List<String> getCapabilities(AwsConfig awsConfig, String region, String data, String type) {
    try {
      AmazonCloudFormationClient client = getAmazonCloudFormationClient(Regions.fromName(region),
          awsConfig.getAccessKey(), awsConfig.getSecretKey(), awsConfig.isUseEc2IamCredentials());
      GetTemplateSummaryRequest request = new GetTemplateSummaryRequest();
      if ("s3".equalsIgnoreCase(type)) {
        request.withTemplateURL(data);
      } // TODO: what needs to done here
      else {
        request.withTemplateBody(data);
      }
      GetTemplateSummaryResult result = client.getTemplateSummary(request);
      return result.getCapabilities();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return emptyList();
  }

  private String getRequestDataFromFile(String path) {
    Path jsonPath = Paths.get(path);
    try {
      List<String> data = Files.readAllLines(jsonPath);
      return String.join("\n", data);
    } catch (IOException ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, "Error in cloning git repo");
    }
  }

  private GitOperationContext cloneRepo(
      GitConfig gitConfig, GitFileConfig gitFileConfig, List<EncryptedDataDetail> sourceRepoEncryptionDetails) {
    GitOperationContext gitOperationContext =
        GitOperationContext.builder().gitConfig(gitConfig).gitConnectorId(gitFileConfig.getConnectorId()).build();
    try {
      encryptionService.decrypt(gitConfig, sourceRepoEncryptionDetails);
      gitClient.ensureRepoLocallyClonedAndUpdated(gitOperationContext);
    } catch (RuntimeException ex) {
      throw new WingsException(ErrorCode.GENERAL_ERROR, "Unable to clone git repo");
    }
    return gitOperationContext;
  }

  private String resolveScriptDirectory(GitOperationContext gitOperationContext, String scriptPath) {
    return Paths
        .get(Paths.get(System.getProperty(USER_DIR_KEY)).toString(),
            gitClientHelper.getRepoDirectory(gitOperationContext), scriptPath)
        .toString();
  }
}
