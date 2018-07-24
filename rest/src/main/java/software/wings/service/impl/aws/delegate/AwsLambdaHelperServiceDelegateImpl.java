package software.wings.service.impl.aws.delegate;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Log.LogLevel.ERROR;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.sm.ExecutionStatus.FAILED;
import static software.wings.sm.ExecutionStatus.SUCCESS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.inject.Singleton;

import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.AWSLambdaClientBuilder;
import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.Environment;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.LogType;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.AwsLambdaContextElement.FunctionMeta;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.exception.InvalidRequestException;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionResponse.AwsLambdaExecuteFunctionResponseBuilder;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfResponse.AwsLambdaExecuteWfResponseBuilder;
import software.wings.service.impl.aws.model.AwsLambdaFunctionParams;
import software.wings.service.impl.aws.model.AwsLambdaFunctionResult;
import software.wings.service.impl.aws.model.AwsLambdaVpcConfig;
import software.wings.service.intfc.aws.delegate.AwsLambdaHelperServiceDelegate;
import software.wings.sm.ExecutionStatus;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class AwsLambdaHelperServiceDelegateImpl
    extends AwsHelperServiceDelegateBase implements AwsLambdaHelperServiceDelegate {
  private static final Logger logger = LoggerFactory.getLogger(AwsLambdaHelperServiceDelegateImpl.class);

  @VisibleForTesting
  AWSLambdaClient getAmazonLambdaClient(String region, String accessKey, char[] secretKey) {
    return (AWSLambdaClient) AWSLambdaClientBuilder.standard()
        .withRegion(region)
        .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, new String(secretKey))))
        .build();
  }

  @Override
  public AwsLambdaExecuteFunctionResponse executeFunction(AwsLambdaExecuteFunctionRequest request) {
    try {
      AwsConfig awsConfig = request.getAwsConfig();
      List<EncryptedDataDetail> encryptionDetails = request.getEncryptionDetails();
      encryptionService.decrypt(awsConfig, encryptionDetails);
      AWSLambdaClient lambdaClient =
          getAmazonLambdaClient(request.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
      InvokeRequest invokeRequest = new InvokeRequest()
                                        .withFunctionName(request.getFunctionName())
                                        .withQualifier(request.getQualifier())
                                        .withLogType(LogType.Tail);
      if (isNotEmpty(request.getPayload())) {
        invokeRequest.setPayload(request.getPayload());
      }
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
      return responseBuilder.build();
    } catch (AmazonServiceException amazonServiceException) {
      handleAmazonServiceException(amazonServiceException);
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
      AWSLambdaClient lambdaClient =
          getAmazonLambdaClient(request.getRegion(), awsConfig.getAccessKey(), awsConfig.getSecretKey());
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
              AwsLambdaFunctionResult.builder().success(false).errorMessage(ex.getMessage()).build());
        }
      }
      responseBuilder.executionStatus(status);
      responseBuilder.functionResults(functionResultList);
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
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
    evaluatedAliases.forEach(alias -> {
      logCallback.saveExecutionLog(format("Creating Function Alias: [%s]", alias));
      CreateAliasResult createAliasResult = lambdaClient.createAlias(
          new CreateAliasRequest().withFunctionName(functionName).withFunctionVersion(functionVersion).withName(alias));
      logCallback.saveExecutionLog(format("Created Function Alias with name:[%s], arn:[%s]",
          createAliasResult.getName(), createAliasResult.getAliasArn()));
    });
  }

  private void updateFunctionAlias(AWSLambdaClient lambdaClient, String functionName, String functionArn,
      List<String> updateAlias, ExecutionLogCallback logCallback) {
    updateAlias.forEach(alias -> {
      logCallback.saveExecutionLog(format("Updating Function Alias: [%s]", alias));
      UpdateAliasResult updateAliasResult = lambdaClient.updateAlias(
          new UpdateAliasRequest().withFunctionName(functionName).withFunctionVersion(functionArn).withName(alias));
      logCallback.saveExecutionLog(format("Updated Function Alias with name:[%s], arn:[%s]",
          updateAliasResult.getName(), updateAliasResult.getAliasArn()));
    });
  }

  private AwsLambdaFunctionResult executeFunctionDeployment(AWSLambdaClient lambdaClient, String roleArn,
      List<String> evaluatedAliases, Map<String, String> serviceVariables, AwsLambdaVpcConfig lambdaVpcConfig,
      AwsLambdaFunctionParams functionParams, ExecutionLogCallback logCallback) {
    logCallback.saveExecutionLog("Deploying Lambda with following configuration", INFO);
    logCallback.saveExecutionLog("Function Name: " + functionParams.getFunctionName(), INFO);
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
      functionResult =
          lambdaClient.getFunction(new GetFunctionRequest().withFunctionName(functionParams.getFunctionName()));
    } catch (ResourceNotFoundException exception) {
      // Function does not exist
    }

    if (functionResult == null) {
      logCallback.saveExecutionLog("Function: " + functionParams.getFunctionName() + " does not exist.", INFO);
      CreateFunctionRequest createFunctionRequest =
          new CreateFunctionRequest()
              .withEnvironment(new Environment().withVariables(serviceVariables))
              .withRuntime(functionParams.getRuntime())
              .withFunctionName(functionParams.getFunctionName())
              .withHandler(functionParams.getHandler())
              .withRole(roleArn)
              .withCode(new FunctionCode().withS3Bucket(functionParams.getBucket()).withS3Key(functionParams.getKey()))
              .withPublish(true)
              .withTimeout(functionParams.getTimeout())
              .withMemorySize(functionParams.getMemory())
              .withVpcConfig(vpcConfig);
      CreateFunctionResult createFunctionResult = lambdaClient.createFunction(createFunctionRequest);
      logCallback.saveExecutionLog(format("Function [%s] published with version [%s] successfully",
                                       functionParams.getFunctionName(), createFunctionResult.getVersion()),
          INFO);
      logCallback.saveExecutionLog(
          format("Created Function Code Sha256: [%s]", createFunctionResult.getCodeSha256()), INFO);
      logCallback.saveExecutionLog(format("Created Function ARN: [%s]", createFunctionResult.getFunctionArn()), INFO);
      createFunctionAlias(lambdaClient, functionParams.getFunctionName(), createFunctionResult.getVersion(),
          evaluatedAliases, logCallback);
      functionMeta = FunctionMeta.newBuilder()
                         .withFunctionArn(createFunctionResult.getFunctionArn())
                         .withFunctionName(createFunctionResult.getFunctionName())
                         .withVersion(createFunctionResult.getVersion())
                         .build();
    } else {
      logCallback.saveExecutionLog(
          format("Function: [%s] exists. Update and Publish", functionParams.getFunctionName()));
      logCallback.saveExecutionLog(
          format("Existing Lambda Function Code Sha256: [%s].", functionResult.getConfiguration().getCodeSha256()));
      UpdateFunctionCodeResult updateFunctionCodeResultDryRun =
          lambdaClient.updateFunctionCode(new UpdateFunctionCodeRequest()
                                              .withFunctionName(functionParams.getFunctionName())
                                              .withPublish(true)
                                              .withS3Bucket(functionParams.getBucket())
                                              .withS3Key(functionParams.getKey()));
      logCallback.saveExecutionLog(
          format("New Lambda function code Sha256: [%s]", updateFunctionCodeResultDryRun.getCodeSha256()));
      if (updateFunctionCodeResultDryRun.getCodeSha256().equals(functionResult.getConfiguration().getCodeSha256())) {
        logCallback.saveExecutionLog("Function code didn't change. Skip function code update", INFO);
      } else {
        UpdateFunctionCodeRequest updateFunctionCodeRequest = new UpdateFunctionCodeRequest()
                                                                  .withFunctionName(functionParams.getFunctionName())
                                                                  .withS3Bucket(functionParams.getBucket())
                                                                  .withS3Key(functionParams.getKey());
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
              .withFunctionName(functionParams.getFunctionName())
              .withHandler(functionParams.getHandler())
              .withRole(roleArn)
              .withTimeout(functionParams.getTimeout())
              .withMemorySize(functionParams.getMemory())
              .withVpcConfig(vpcConfig);
      UpdateFunctionConfigurationResult updateFunctionConfigurationResult =
          lambdaClient.updateFunctionConfiguration(updateFunctionConfigurationRequest);
      logCallback.saveExecutionLog("Function configuration updated successfully", INFO);
      logCallback.saveExecutionLog("Publishing new version", INFO);
      PublishVersionRequest publishVersionRequest =
          new PublishVersionRequest()
              .withFunctionName(updateFunctionConfigurationResult.getFunctionName())
              .withCodeSha256(updateFunctionConfigurationResult.getCodeSha256());
      PublishVersionResult publishVersionResult = lambdaClient.publishVersion(publishVersionRequest);
      logCallback.saveExecutionLog(format("Published new version: [%s]", publishVersionResult.getVersion()));
      logCallback.saveExecutionLog(format("Published function ARN: [%s]", publishVersionResult.getFunctionArn()));
      ListAliasesResult listAliasesResult =
          lambdaClient.listAliases(new ListAliasesRequest().withFunctionName(functionParams.getFunctionName()));
      List<String> newAliases = evaluatedAliases.stream()
                                    .filter(alias
                                        -> listAliasesResult.getAliases().stream().noneMatch(
                                            aliasConfiguration -> aliasConfiguration.getName().equals(alias)))
                                    .collect(toList());
      if (isNotEmpty(newAliases)) {
        createFunctionAlias(
            lambdaClient, functionParams.getFunctionName(), publishVersionResult.getVersion(), newAliases, logCallback);
      }
      List<String> updateAlias =
          evaluatedAliases.stream()
              .filter(alias -> newAliases != null && newAliases.stream().noneMatch(s -> s.equals(alias)))
              .collect(toList());
      if (isNotEmpty(updateAlias)) {
        updateFunctionAlias(lambdaClient, functionParams.getFunctionName(), publishVersionResult.getVersion(),
            updateAlias, logCallback);
      }
      functionMeta = FunctionMeta.newBuilder()
                         .withFunctionArn(publishVersionResult.getFunctionArn())
                         .withFunctionName(publishVersionResult.getFunctionName())
                         .withVersion(publishVersionResult.getVersion())
                         .build();
    }
    logCallback.saveExecutionLog(
        format("Successfully deployed lambda function: [%s]", functionParams.getFunctionName()));
    logCallback.saveExecutionLog("=================");
    return AwsLambdaFunctionResult.builder().success(true).functionMeta(functionMeta).build();
  }
}