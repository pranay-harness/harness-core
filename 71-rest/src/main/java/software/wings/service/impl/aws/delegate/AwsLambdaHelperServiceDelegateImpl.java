package software.wings.service.impl.aws.delegate;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.ListFunctionsRequest;
import com.amazonaws.services.lambda.model.ListFunctionsResult;
import com.amazonaws.services.lambda.model.LogType;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.TagResourceRequest;
import com.amazonaws.services.lambda.model.UntagResourceRequest;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.util.Strings;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse.AwsLambdaExecuteFunctionResponseBuilder;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse.AwsLambdaExecuteWfResponseBuilder;
import software.wings.service.impl.aws.model.AwsLambdaFunctionParams;
import software.wings.service.impl.aws.model.AwsLambdaFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResponse.AwsLambdaFunctionResponseBuilder;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResult;
import software.wings.service.impl.aws.model.AwsLambdaVpcConfig;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

@Singleton
@Slf4j
public class AwsLambdaHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsLambdaHelperServiceDelegate {
  @VisibleForTesting
  public AWSLambdaClient getAmazonLambdaClient(String region, AwsConfig awsConfig) {
    AWSLambdaClientBuilder builder = AWSLambdaClientBuilder.standard().withRegion(region);
    attachCredentials(builder, awsConfig);
    return (AWSLambdaClient) builder.build();
  }

  @Override
  public AwsLambdaExecuteFunctionResponse executeFunction(AwsLambdaExecuteFunctionRequest request) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AWSLambdaClient lambdaClient = getAmazonLambdaClient(request.getRegion(), awsConfig);
      InvokeRequest invokeRequest = new InvokeRequest()
                                        .withFunctionName(request.getFunctionName())
                                        .withQualifier(request.getQualifier())
                                        .withLogType(LogType.Tail);
      if (isNotEmpty(request.getPayload())) {
        invokeRequest.setPayload(request.getPayload());
      }
      tracker.trackLambdaCall("Invoke Function");
      InvokeResult invokeResult = lambdaClient.invoke(invokeRequest);
      logger.info("Lambda invocation result: " + invokeResult.toString());
      AwsLambdaExecuteFunctionResponseBuilder responseBuilder = AwsLambdaExecuteFunctionResponse.builder();
      responseBuilder.statusCode(invokeResult.getStatusCode());
      responseBuilder.functionError(invokeResult.getFunctionError());
      String logResult = invokeResult.getLogResult();
      if (logResult != null) {
        try {
          logResult = new String(Base64.decodeBase64(logResult), "UTF-8");
          responseBuilder.logResult(logResult);
        } catch (UnsupportedEncodingException ex) {
          throw new WingsException(ex);
        }
      }
      responseBuilder.payload(StandardCharsets.UTF_8.decode(invokeResult.getPayload()).toString());
      responseBuilder.awsLambdaExecutionData(request.getAwsLambdaExecutionData())
          .lambdaTestEvent(request.getLambdaTestEvent())
          .executionStatus(SUCCESS);
      return responseBuilder.build();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  @Override
  public AwsLambdaFunctionResponse getLambdaFunctions(AwsLambdaFunctionRequest request) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AWSLambdaClient lambdaClient = getAmazonLambdaClient(request.getRegion(), awsConfig);
      AwsLambdaFunctionResponseBuilder response = AwsLambdaFunctionResponse.builder();
      List<String> lambdaFunctions = new ArrayList<>();
      List<FunctionConfiguration> functionConfigurations = new ArrayList<>();

      ListFunctionsResult listFunctionsResult = null;
      String nextMarker = null;
      do {
        tracker.trackLambdaCall("List Functions");
        listFunctionsResult =
            lambdaClient.listFunctions(new ListFunctionsRequest().withMaxItems(100).withMarker(nextMarker));
        functionConfigurations.addAll(listFunctionsResult.getFunctions());
        nextMarker = listFunctionsResult.getNextMarker();
      } while (nextMarker != null);

      functionConfigurations.forEach(
          functionConfiguration -> lambdaFunctions.add(functionConfiguration.getFunctionName()));
      return response.lambdaFunctions(lambdaFunctions).executionStatus(SUCCESS).build();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }

  @Override
  public AwsLambdaExecuteWfResponse executeWf(AwsLambdaExecuteWfRequest request, ExecutionLogCallback logCallback) {
    AwsLambdaExecuteWfResponseBuilder responseBuilder = AwsLambdaExecuteWfResponse.builder();
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      responseBuilder.awsConfig(awsConfig);
      responseBuilder.region(request.getRegion());
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AWSLambdaClient lambdaClient = getAmazonLambdaClient(request.getRegion(), awsConfig);
      String roleArn = request.getRoleArn();
      List<String> evaluatedAliases = request.getEvaluatedAliases();
      Map<String, String> serviceVariables = request.getServiceVariables();
      AwsLambdaVpcConfig lambdaVpcConfig = request.getLambdaVpcConfig();
      List<AwsLambdaFunctionParams> functionParamsList = request.getFunctionParams();
      List<AwsLambdaFunctionResult> functionResultList = new ArrayList<>();
      ExecutionStatus status = SUCCESS;
      for (AwsLambdaFunctionParams functionParams : functionParamsList) {
        try {
          functionResultList.add(executeFunctionDeployment(
              lambdaClient, roleArn, evaluatedAliases, serviceVariables, lambdaVpcConfig, functionParams, logCallback));
        } catch (Exception ex) {
          logCallback.saveExecutionLog(
              "Exception: " + ex.getMessage() + " while deploying function: " + functionParams.getFunctionName(),
              ERROR);
          status = FAILED;
          functionResultList.add(
              AwsLambdaFunctionResult.builder()
                  .success(false)
                  .errorMessage(ex.getMessage())
                  .functionMeta(FunctionMeta.builder().functionName(functionParams.getFunctionName()).build())
                  .build());
        }
      }
      responseBuilder.executionStatus(status);
      responseBuilder.functionResults(functionResultList);

      String message = "Successfully completed Aws Lambda Deploy step";
      CommandExecutionStatus finalStatus = CommandExecutionStatus.SUCCESS;
      LogLevel level = INFO;
      if (FAILED == status) {
        message = "Failed while deploying Lambda functions";
        finalStatus = CommandExecutionStatus.FAILURE;
        level = ERROR;
      }
      logCallback.saveExecutionLog(message, level, finalStatus);

    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return responseBuilder.build();
  }

  private VpcConfig getVpcConfig(AwsLambdaVpcConfig awsLambdaVpcConfig) {
    String vpcId = awsLambdaVpcConfig.getVpcId();
    VpcConfig vpcConfig = new VpcConfig();
    if (vpcId != null) {
      List<String> subnetIds = awsLambdaVpcConfig.getSubnetIds();
      List<String> securityGroupIds = awsLambdaVpcConfig.getSecurityGroupIds();
      if (!securityGroupIds.isEmpty() && !subnetIds.isEmpty()) {
        vpcConfig.setSubnetIds(subnetIds);
        vpcConfig.setSecurityGroupIds(securityGroupIds);
      } else {
        throw new InvalidRequestException("At least one security group and one subnet must be provided");
      }
    }
    return vpcConfig;
  }

  private void createFunctionAlias(AWSLambdaClient lambdaClient, String functionName, String functionVersion,
      List<String> evaluatedAliases, ExecutionLogCallback logCallback) {
    if (isNotEmpty(evaluatedAliases)) {
      evaluatedAliases.forEach(alias -> {
        logCallback.saveExecutionLog(format("Creating Function Alias: [%s]", alias));
        tracker.trackLambdaCall("Create Function Alias");
        CreateAliasResult createAliasResult = lambdaClient.createAlias(new CreateAliasRequest()
                                                                           .withFunctionName(functionName)
                                                                           .withFunctionVersion(functionVersion)
                                                                           .withName(alias));
        logCallback.saveExecutionLog(format("Created Function Alias with name:[%s], arn:[%s]",
            createAliasResult.getName(), createAliasResult.getAliasArn()));
      });
    }
  }

  private void updateFunctionAlias(AWSLambdaClient lambdaClient, String functionName, String functionArn,
      List<String> updateAlias, ExecutionLogCallback logCallback) {
    updateAlias.forEach(alias -> {
      logCallback.saveExecutionLog(format("Updating Function Alias: [%s]", alias));
      tracker.trackLambdaCall("Update Function Alias");
      UpdateAliasResult updateAliasResult = lambdaClient.updateAlias(
          new UpdateAliasRequest().withFunctionName(functionName).withFunctionVersion(functionArn).withName(alias));
      logCallback.saveExecutionLog(format("Updated Function Alias with name:[%s], arn:[%s]",
          updateAliasResult.getName(), updateAliasResult.getAliasArn()));
    });
  }

  /**
   * Intially Aws Lambda did not allow for _ to be in the function name. So on the
   * Harness side we were normalizing it to -. But now _ is allowed. So now we need
   * special handling in case we may be updating existing name where _ may have
   * been normalized to -.
   */
  @VisibleForTesting
  String getAlternateNormalizedFunctionName(String functionName) {
    return functionName.replace('_', '-');
  }

  private AwsLambdaFunctionResult executeFunctionDeployment(AWSLambdaClient lambdaClient, String roleArn,
      List<String> evaluatedAliases, Map<String, String> serviceVariables, AwsLambdaVpcConfig lambdaVpcConfig,
      AwsLambdaFunctionParams functionParams, ExecutionLogCallback logCallback) {
    String functionName = functionParams.getFunctionName();
    logCallback.saveExecutionLog("Deploying Lambda with following configuration", INFO);
    logCallback.saveExecutionLog("Function Name: " + functionName, INFO);
    logCallback.saveExecutionLog("S3 Bucket: " + functionParams.getBucket(), INFO);
    logCallback.saveExecutionLog("Bucket Key: " + functionParams.getKey(), INFO);
    logCallback.saveExecutionLog("Function handler: " + functionParams.getHandler(), INFO);
    logCallback.saveExecutionLog("Function runtime: " + functionParams.getRuntime(), INFO);
    logCallback.saveExecutionLog("Function memory: " + functionParams.getMemory(), INFO);
    logCallback.saveExecutionLog("Function execution timeout: " + functionParams.getTimeout(), INFO);
    logCallback.saveExecutionLog("IAM role ARN: " + roleArn, INFO);
    logCallback.saveExecutionLog("VPC: " + lambdaVpcConfig.getVpcId(), INFO);
    if (isNotEmpty(lambdaVpcConfig.getSubnetIds())) {
      logCallback.saveExecutionLog("Subnet: " + Joiner.on(",").join(lambdaVpcConfig.getSubnetIds(), INFO));
    }
    if (isNotEmpty(lambdaVpcConfig.getSecurityGroupIds())) {
      logCallback.saveExecutionLog(
          "Security Groups: " + Joiner.on(",").join(lambdaVpcConfig.getSecurityGroupIds()), INFO);
    }
    if (isNotEmpty(evaluatedAliases)) {
      logCallback.saveExecutionLog("Function Aliases: " + Joiner.on(",").join(evaluatedAliases), INFO);
    }

    FunctionMeta functionMeta;
    VpcConfig vpcConfig = getVpcConfig(lambdaVpcConfig);
    GetFunctionResult functionResult = null;
    try {
      logCallback.saveExecutionLog(format("Testing existence of function with name: [%s]", functionName));
      tracker.trackLambdaCall("Get Function");
      functionResult = lambdaClient.getFunction(new GetFunctionRequest().withFunctionName(functionName));
    } catch (ResourceNotFoundException exception) {
      // Function does not exist
      logCallback.saveExecutionLog(format("Function: [%s] not found.", functionName));
    }

    if (functionResult == null) {
      String alternateNormalizedFunctionName = getAlternateNormalizedFunctionName(functionName);
      if (!alternateNormalizedFunctionName.equals(functionName)) {
        try {
          logCallback.saveExecutionLog(
              format("Testing alternate function name: [%s] for existence", alternateNormalizedFunctionName));
          tracker.trackLambdaCall("Get Function");
          functionResult =
              lambdaClient.getFunction(new GetFunctionRequest().withFunctionName(alternateNormalizedFunctionName));
        } catch (ResourceNotFoundException exception) {
          // Function does not exist
          logCallback.saveExecutionLog(format("Function: [%s] not found.", alternateNormalizedFunctionName));
        }
        if (functionResult != null) {
          logCallback.saveExecutionLog(
              format("Found existing function with name: [%s]. Using this.", alternateNormalizedFunctionName));
          functionName = alternateNormalizedFunctionName;
        }
      }
    }

    if (functionResult == null) {
      logCallback.saveExecutionLog("Function: " + functionName + " does not exist.", INFO);
      CreateFunctionRequest createFunctionRequest =
          new CreateFunctionRequest()
              .withEnvironment(new Environment().withVariables(serviceVariables))
              .withRuntime(functionParams.getRuntime())
              .withFunctionName(functionName)
              .withHandler(functionParams.getHandler())
              .withRole(roleArn)
              .withCode(new FunctionCode().withS3Bucket(functionParams.getBucket()).withS3Key(functionParams.getKey()))
              .withPublish(true)
              .withTags(functionParams.getFunctionTags())
              .withTimeout(functionParams.getTimeout())
              .withMemorySize(functionParams.getMemory())
              .withVpcConfig(vpcConfig);
      tracker.trackLambdaCall("Create Function");
      CreateFunctionResult createFunctionResult = lambdaClient.createFunction(createFunctionRequest);
      logCallback.saveExecutionLog(format("Function [%s] published with version [%s] successfully", functionName,
                                       createFunctionResult.getVersion()),
          INFO);
      logCallback.saveExecutionLog(
          format("Created Function Code Sha256: [%s]", createFunctionResult.getCodeSha256()), INFO);
      logCallback.saveExecutionLog(format("Created Function ARN: [%s]", createFunctionResult.getFunctionArn()), INFO);
      createFunctionAlias(lambdaClient, functionName, createFunctionResult.getVersion(), evaluatedAliases, logCallback);
      functionMeta = FunctionMeta.builder()
                         .functionArn(createFunctionResult.getFunctionArn())
                         .functionName(createFunctionResult.getFunctionName())
                         .version(createFunctionResult.getVersion())
                         .build();
    } else {
      logCallback.saveExecutionLog(format("Function: [%s] exists. Update and Publish", functionName));
      logCallback.saveExecutionLog(
          format("Existing Lambda Function Code Sha256: [%s].", functionResult.getConfiguration().getCodeSha256()));
      tracker.trackLambdaCall("Update Function Code");
      UpdateFunctionCodeResult updateFunctionCodeResultDryRun =
          lambdaClient.updateFunctionCode(new UpdateFunctionCodeRequest()
                                              .withFunctionName(functionName)
                                              .withPublish(true)
                                              .withS3Bucket(functionParams.getBucket())
                                              .withS3Key(functionParams.getKey()));
      logCallback.saveExecutionLog(
          format("New Lambda function code Sha256: [%s]", updateFunctionCodeResultDryRun.getCodeSha256()));
      if (updateFunctionCodeResultDryRun.getCodeSha256().equals(functionResult.getConfiguration().getCodeSha256())) {
        logCallback.saveExecutionLog("Function code didn't change. Skip function code update", INFO);
      } else {
        UpdateFunctionCodeRequest updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
                                                                  .withFunctionName(functionName)
                                                                  .withS3Bucket(functionParams.getBucket())
                                                                  .withS3Key(functionParams.getKey());
        tracker.trackLambdaCall("Update Function code");
        UpdateFunctionCodeResult updateFunctionCodeResult = lambdaClient.updateFunctionCode(updateFunctionCodeRequest);
        logCallback.saveExecutionLog("Function code updated successfully", INFO);
        logCallback.saveExecutionLog(
            format("Updated Function Code Sha256: [%s]", updateFunctionCodeResult.getCodeSha256()));
        logCallback.saveExecutionLog(format("Updated Function ARN: [%s]", updateFunctionCodeResult.getFunctionArn()));
      }

      logCallback.saveExecutionLog("Updating function configuration", INFO);
      UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest =
          new UpdateFunctionConfigurationRequest()
              .withEnvironment(new Environment().withVariables(serviceVariables))
              .withRuntime(functionParams.getRuntime())
              .withFunctionName(functionName)
              .withHandler(functionParams.getHandler())
              .withRole(roleArn)
              .withTimeout(functionParams.getTimeout())
              .withMemorySize(functionParams.getMemory())
              .withVpcConfig(vpcConfig);
      tracker.trackLambdaCall("Update Function Configuration");
      UpdateFunctionConfigurationResult updateFunctionConfigurationResult =
          lambdaClient.updateFunctionConfiguration(updateFunctionConfigurationRequest);
      logCallback.saveExecutionLog("Function configuration updated successfully", INFO);
      logCallback.saveExecutionLog("Publishing new version", INFO);
      PublishVersionRequest publishVersionRequest =
          new PublishVersionRequest()
              .withFunctionName(updateFunctionConfigurationResult.getFunctionName())
              .withCodeSha256(updateFunctionConfigurationResult.getCodeSha256());
      tracker.trackLambdaCall("Publish Function Version");
      PublishVersionResult publishVersionResult = lambdaClient.publishVersion(publishVersionRequest);
      logCallback.saveExecutionLog(format("Published new version: [%s]", publishVersionResult.getVersion()));
      logCallback.saveExecutionLog(format("Published function ARN: [%s]", publishVersionResult.getFunctionArn()));
      ListAliasesResult listAliasesResult =
          lambdaClient.listAliases(new ListAliasesRequest().withFunctionName(functionName));

      List<String> newAliases = new ArrayList<>();
      if (isNotEmpty(evaluatedAliases)) {
        newAliases.addAll(evaluatedAliases.stream()
                              .filter(alias
                                  -> listAliasesResult.getAliases().stream().noneMatch(
                                      aliasConfiguration -> aliasConfiguration.getName().equals(alias)))
                              .collect(toList()));
      }
      if (isNotEmpty(newAliases)) {
        createFunctionAlias(lambdaClient, functionName, publishVersionResult.getVersion(), newAliases, logCallback);
      }

      List<String> updateAlias = new ArrayList<>();
      if (isNotEmpty(evaluatedAliases)) {
        updateAlias.addAll(
            evaluatedAliases.stream()
                .filter(alias -> newAliases != null && newAliases.stream().noneMatch(s -> s.equals(alias)))
                .collect(toList()));
      }
      if (isNotEmpty(updateAlias)) {
        updateFunctionAlias(lambdaClient, functionName, publishVersionResult.getVersion(), updateAlias, logCallback);
      }
      tagExistingFunction(functionResult, functionParams.getFunctionTags(), logCallback, lambdaClient);

      functionMeta = FunctionMeta.builder()
                         .functionArn(publishVersionResult.getFunctionArn())
                         .functionName(publishVersionResult.getFunctionName())
                         .version(publishVersionResult.getVersion())
                         .build();
    }
    logCallback.saveExecutionLog(format("Successfully deployed lambda function: [%s]", functionName));
    logCallback.saveExecutionLog("=================");
    return AwsLambdaFunctionResult.builder().success(true).functionMeta(functionMeta).build();
  }

  @VisibleForTesting
  void tagExistingFunction(GetFunctionResult functionResult, Map<String, String> functionTags,
      ExecutionLogCallback logCallback, AWSLambdaClient lambdaClient) {
    String functionArn = functionResult.getConfiguration().getFunctionArn();
    Map<String, String> existingTags = functionResult.getTags();
    if (isNotEmpty(existingTags)) {
      List<String> keysToRemove =
          existingTags.entrySet().stream().map(Entry::getKey).filter(key -> !key.startsWith("aws:")).collect(toList());
      if (isNotEmpty(keysToRemove)) {
        logCallback.saveExecutionLog(format("Untagging existing tags from the function: [%s]", functionArn));
        tracker.trackLambdaCall("Untag Function");
        lambdaClient.untagResource(new UntagResourceRequest().withResource(functionArn).withTagKeys(keysToRemove));
      }
    }
    if (isEmpty(functionTags)) {
      logCallback.saveExecutionLog("No new tags to be put.");
      return;
    }
    logCallback.saveExecutionLog(format("Executing tagging for function: [%s]", functionArn));
    tracker.trackLambdaCall("Tag Function");
    lambdaClient.tagResource(new TagResourceRequest().withResource(functionArn).withTags(functionTags));
  }

  @Override
  public AwsLambdaDetailsResponse getFunctionDetails(AwsLambdaDetailsRequest request) {
    try {
      GetFunctionResult getFunctionResult = null;
      final AwsConfig awsConfig = request.getAwsConfig();
      final List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      final AWSLambdaClient lambdaClient = getAmazonLambdaClient(request.getRegion(), awsConfig);
      try {
        tracker.trackLambdaCall("Get Function");
        getFunctionResult = lambdaClient.getFunction(
            new GetFunctionRequest().withFunctionName(request.getFunctionName()).withQualifier(request.getQualifier()));
      } catch (ResourceNotFoundException rnfe) {
        logger.info("No function found with name =[{}], qualifier =[{}]. Error Msg is [{}]", request.getFunctionName(),
            request.getQualifier(), rnfe.getMessage());
        return AwsLambdaDetailsResponse.builder().executionStatus(SUCCESS).details(null).build();
      }
      ListAliasesResult listAliasesResult = null;
      if (Boolean.TRUE.equals(request.getLoadAliases())) {
        final ListAliasesRequest listAliasRequest =
            new ListAliasesRequest().withFunctionName(request.getFunctionName());
        if (Strings.isNotEmpty(getFunctionResult.getConfiguration().getVersion())) {
          listAliasRequest.withFunctionVersion(getFunctionResult.getConfiguration().getVersion());
        }
        tracker.trackLambdaCall("List Function Aliases");
        listAliasesResult = lambdaClient.listAliases(listAliasRequest);
      }
      return AwsLambdaDetailsResponse.from(getFunctionResult, listAliasesResult);
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }
    return null;
  }
}