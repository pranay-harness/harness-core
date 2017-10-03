package software.wings.sm.states;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.common.base.Joiner;
import com.google.inject.Inject;

import com.amazonaws.services.lambda.model.CreateAliasRequest;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.PublishVersionRequest;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.UpdateAliasRequest;
import com.amazonaws.services.lambda.model.UpdateAliasResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import com.amazonaws.services.lambda.model.VpcConfig;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.CommandStateExecutionData;
import software.wings.api.PhaseElement;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.AwsConfig;
import software.wings.beans.AwsLambdaInfraStructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.LambdaSpecification;
import software.wings.beans.Log.Builder;
import software.wings.beans.Log.LogLevel;
import software.wings.beans.Service;
import software.wings.beans.ServiceVariable;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.command.CommandUnit;
import software.wings.cloudprovider.aws.AwsLambdaService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateExecutionException;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The type Aws lambda state.
 */
public class AwsLambdaState extends State {
  /**
   * The Settings service.
   */
  @Inject @Transient protected transient SettingsService settingsService;

  /**
   * The Service resource service.
   */
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;

  /**
   * The Service template service.
   */
  @Inject @Transient protected transient ServiceTemplateService serviceTemplateService;
  /**
   * The Activity service.
   */
  @Inject @Transient protected transient ActivityService activityService;

  /**
   * The Infrastructure mapping service.
   */
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient private transient AwsHelperService awsHelperService;

  @Inject @Transient private transient AwsLambdaService awsLambdaService;

  @Inject @Transient private transient LogService logService;

  @Inject @Transient private transient ArtifactStreamService artifactStreamService;

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue(Constants.AWS_LAMBDA_COMMAND_NAME)
  private String commandName = Constants.AWS_LAMBDA_COMMAND_NAME;

  @Attributes(title = "Lambda Function Alias", required = true)
  @DefaultValue("${env.name}")
  @SchemaIgnore
  private List<String> aliases = new ArrayList<>();

  /**
   * Instantiates a new Aws lambda state.
   *
   * @param name the name
   */
  public AwsLambdaState(String name) {
    super(name, StateType.AWS_LAMBDA_STATE.name());
  }

  /**
   * Instantiates a new Aws lambda state.
   *
   * @param name the name
   * @param type the type
   */
  protected AwsLambdaState(String name, String type) {
    super(name, type);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    PhaseElement phaseElement = context.getContextElement(ContextElementType.PARAM, Constants.PHASE_PARAM);
    String serviceId = phaseElement.getServiceElement().getUuid();

    WorkflowStandardParams workflowStandardParams = context.getContextElement(ContextElementType.STANDARD);
    Application app = workflowStandardParams.getApp();
    Environment env = workflowStandardParams.getEnv();

    String envId = env.getUuid();
    Service service = serviceResourceService.get(app.getUuid(), serviceId);
    Command command =
        serviceResourceService.getCommandByName(app.getUuid(), serviceId, envId, getCommandName()).getCommand();

    LambdaSpecification lambdaSpecification =
        serviceResourceService.getLambdaSpecification(app.getUuid(), service.getUuid());

    AwsLambdaInfraStructureMapping infrastructureMapping =
        (AwsLambdaInfraStructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String region = infrastructureMapping.getRegion();

    List<CommandUnit> commandUnitList =
        serviceResourceService.getFlattenCommandUnitList(app.getUuid(), serviceId, envId, command.getName());
    Activity.Builder activityBuilder = Activity.Builder.anActivity()
                                           .withAppId(app.getUuid())
                                           .withApplicationName(app.getName())
                                           .withEnvironmentId(envId)
                                           .withEnvironmentName(env.getName())
                                           .withEnvironmentType(env.getEnvironmentType())
                                           .withServiceId(service.getUuid())
                                           .withServiceName(service.getName())
                                           .withCommandName(command.getName())
                                           .withType(Type.Command)
                                           .withWorkflowExecutionId(context.getWorkflowExecutionId())
                                           .withWorkflowType(context.getWorkflowType())
                                           .withWorkflowExecutionName(context.getWorkflowExecutionName())
                                           .withStateExecutionInstanceId(context.getStateExecutionInstanceId())
                                           .withStateExecutionInstanceName(context.getStateExecutionInstanceName())
                                           .withCommandUnits(commandUnitList)
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    Artifact artifact = getArtifact(app.getUuid(), serviceId, context.getWorkflowExecutionId(), workflowStandardParams);
    if (artifact == null) {
      throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
    }
    ArtifactStream artifactStream = artifactStreamService.get(artifact.getAppId(), artifact.getArtifactStreamId());

    activityBuilder.withArtifactStreamId(artifactStream.getUuid())
        .withArtifactStreamName(artifactStream.getSourceName())
        .withArtifactName(artifact.getDisplayName())
        .withArtifactId(artifact.getUuid());
    activityBuilder.withArtifactId(artifact.getUuid()).withArtifactName(artifact.getDisplayName());

    Activity activity = activityService.save(activityBuilder.build());

    Builder logBuilder = aLog()
                             .withAppId(activity.getAppId())
                             .withActivityId(activity.getUuid())
                             .withLogLevel(LogLevel.INFO)
                             .withCommandUnitName(commandUnitList.get(0).getName())
                             .withExecutionResult(CommandExecutionStatus.RUNNING);

    logService.save(logBuilder.but().withLogLine("Begin command execution.").build());

    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData()
                                                                 .withServiceId(service.getUuid())
                                                                 .withServiceName(service.getName())
                                                                 .withAppId(app.getUuid())
                                                                 .withCommandName(getCommandName())
                                                                 .withActivityId(activity.getUuid());

    String key = context.renderExpression(artifact.getMetadata().get("key"));
    String bucket = context.renderExpression(artifact.getMetadata().get("bucketName"));

    String functionName = context.renderExpression(lambdaSpecification.getFunctionName());
    String handler = context.renderExpression(lambdaSpecification.getHandler());
    String runtime = context.renderExpression(lambdaSpecification.getRuntime());
    Integer memory = lambdaSpecification.getMemorySize();
    Integer timeout = lambdaSpecification.getTimeout();
    String roleArn = infrastructureMapping.getRole();
    List<String> evaluatedAliases = new ArrayList<>();
    if (aliases != null && aliases.size() > 0) {
      evaluatedAliases = aliases.stream().map(context::renderExpression).collect(Collectors.toList());
    }

    logService.save(logBuilder.but().withLogLine("Deploying Lambda with following configuration.").build());
    logService.save(logBuilder.but().withLogLine("Function Name: " + functionName).build());
    logService.save(logBuilder.but().withLogLine("S3 Bucket: " + bucket).build());
    logService.save(logBuilder.but().withLogLine("Bucket key: " + key).build());
    logService.save(logBuilder.but().withLogLine("Function Handler: " + handler).build());
    logService.save(logBuilder.but().withLogLine("Function Runtime: " + runtime).build());
    logService.save(logBuilder.but().withLogLine("Function Memory: " + memory).build());
    logService.save(logBuilder.but().withLogLine("Function Execution Timeout: " + timeout).build());
    logService.save(logBuilder.but().withLogLine("IAM Role Arn: " + roleArn).build());
    logService.save(logBuilder.but().withLogLine("VPC: " + infrastructureMapping.getVpcId()).build());
    logService.save(
        logBuilder.but().withLogLine("Subnet: " + Joiner.on(",").join(infrastructureMapping.getSubnetIds())).build());
    logService.save(
        logBuilder.but()
            .withLogLine("Security Groups: " + Joiner.on(",").join(infrastructureMapping.getSecurityGroupIds()))
            .build());
    logService.save(logBuilder.but().withLogLine("Function Aliases: " + Joiner.on(",").join(evaluatedAliases)).build());

    AwsConfig awsConfig = (AwsConfig) cloudProviderSetting.getValue();

    Map<String, String> serviceVariables =
        serviceTemplateService
            .computeServiceVariables(app.getUuid(), envId, infrastructureMapping.getServiceTemplateId())
            .stream()
            .collect(
                Collectors.toMap(ServiceVariable::getName, sv -> context.renderExpression(new String(sv.getValue()))));

    String accessKey = awsConfig.getAccessKey();
    char[] secretKey = awsConfig.getSecretKey();
    GetFunctionResult functionResult = awsHelperService.getFunction(
        region, accessKey, secretKey, new GetFunctionRequest().withFunctionName(functionName));

    VpcConfig vpcConfig = constructVpcConfig(infrastructureMapping);

    if (functionResult == null) {
      logService.save(
          logBuilder.but().withLogLine(String.format("Function [%s] doesn't exist.", functionName)).build());

      CreateFunctionRequest createFunctionRequest =
          new CreateFunctionRequest()
              .withEnvironment(new com.amazonaws.services.lambda.model.Environment().withVariables(serviceVariables))
              .withRuntime(runtime)
              .withFunctionName(functionName)
              .withHandler(handler)
              .withRole(roleArn)
              .withCode(new FunctionCode().withS3Bucket(bucket).withS3Key(key))
              .withPublish(true)
              .withTimeout(timeout)
              .withMemorySize(memory)
              .withVpcConfig(vpcConfig);

      CreateFunctionResult createFunctionResult =
          awsHelperService.createFunction(region, accessKey, secretKey, createFunctionRequest);
      logService.save(logBuilder.but()
                          .withLogLine(String.format("Function [%s] published with version [%s] successfully",
                              functionName, createFunctionResult.getVersion()))
                          .build());
      logService.save(logBuilder.but()
                          .withLogLine("Created Function Code Sha256: " + createFunctionResult.getCodeSha256())
                          .build());
      logService.save(
          logBuilder.but().withLogLine("Created Function ARN: " + createFunctionResult.getFunctionArn()).build());

      createFunctionAlias(
          region, accessKey, secretKey, functionName, createFunctionResult.getVersion(), evaluatedAliases, logBuilder);
    } else {
      // Update code
      logService.save(logBuilder.but().withLogLine("Function exists. Update and Publish").build());
      // dry run
      logService.save(
          logBuilder.but()
              .withLogLine("Existing Lambda Function Code Sha256: " + functionResult.getConfiguration().getCodeSha256())
              .build());
      UpdateFunctionCodeResult updateFunctionCodeResultDryRun =
          awsHelperService.updateFunctionCode(region, accessKey, secretKey,
              new UpdateFunctionCodeRequest()
                  .withFunctionName(functionName)
                  .withPublish(true)
                  .withS3Bucket(bucket)
                  .withS3Key(key));
      logService.save(
          logBuilder.but()
              .withLogLine("New Lambda Function Code Sha256: " + updateFunctionCodeResultDryRun.getCodeSha256())
              .build());

      if (updateFunctionCodeResultDryRun.getCodeSha256().equals(functionResult.getConfiguration().getCodeSha256())) {
        logService.save(logBuilder.but().withLogLine("Function code didn't change. Skip function code update").build());
      } else {
        UpdateFunctionCodeRequest updateFunctionCodeRequest =
            new UpdateFunctionCodeRequest().withFunctionName(functionName).withS3Bucket(bucket).withS3Key(key);
        UpdateFunctionCodeResult updateFunctionCodeResult =
            awsHelperService.updateFunctionCode(region, accessKey, secretKey, updateFunctionCodeRequest);
        logService.save(logBuilder.but().withLogLine("Function code updated successfully").build());
        logService.save(logBuilder.but()
                            .withLogLine("Updated Function Code Sha256: " + updateFunctionCodeResult.getCodeSha256())
                            .build());
        logService.save(
            logBuilder.but().withLogLine("Updated Function ARN: " + updateFunctionCodeResult.getFunctionArn()).build());
      }
      // update function configuration
      logService.save(logBuilder.but().withLogLine("Updating function configuration").build());
      UpdateFunctionConfigurationRequest updateFunctionConfigurationRequest =
          new UpdateFunctionConfigurationRequest()
              .withEnvironment(new com.amazonaws.services.lambda.model.Environment().withVariables(serviceVariables))
              .withRuntime(runtime)
              .withFunctionName(functionName)
              .withHandler(handler)
              .withRole(roleArn)
              .withTimeout(timeout)
              .withMemorySize(memory)
              .withVpcConfig(vpcConfig);
      UpdateFunctionConfigurationResult updateFunctionConfigurationResult =
          awsHelperService.updateFunctionConfiguration(
              region, accessKey, secretKey, updateFunctionConfigurationRequest);
      logService.save(logBuilder.but().withLogLine("Function configuration updated successfully").build());

      // publish version
      logService.save(logBuilder.but().withLogLine("Publishing new version").build());
      PublishVersionRequest publishVersionRequest =
          new PublishVersionRequest()
              .withFunctionName(updateFunctionConfigurationResult.getFunctionName())
              .withCodeSha256(updateFunctionConfigurationResult.getCodeSha256());
      PublishVersionResult publishVersionResult =
          awsHelperService.publishVersion(region, accessKey, secretKey, publishVersionRequest);
      logService.save(
          logBuilder.but().withLogLine("Published new version: " + publishVersionResult.getVersion()).build());
      logService.save(
          logBuilder.but().withLogLine("Published Function ARN: " + publishVersionResult.getFunctionArn()).build());

      ListAliasesResult listAliasesResult = awsHelperService.listAliases(
          region, accessKey, secretKey, new ListAliasesRequest().withFunctionName(functionName));
      List<String> newAliases = evaluatedAliases.stream()
                                    .filter(alias
                                        -> listAliasesResult.getAliases().stream().noneMatch(
                                            aliasConfiguration -> aliasConfiguration.getName().equals(alias)))
                                    .collect(Collectors.toList());
      if (newAliases != null) {
        createFunctionAlias(
            region, accessKey, secretKey, functionName, publishVersionResult.getVersion(), newAliases, logBuilder);
      }
      List<String> updateAlias =
          evaluatedAliases.stream()
              .filter(alias -> newAliases != null && newAliases.stream().noneMatch(s -> s.equals(alias)))
              .collect(Collectors.toList());
      if (updateAlias != null) {
        updateFunctionAlias(
            region, accessKey, secretKey, functionName, publishVersionResult.getVersion(), updateAlias, logBuilder);
      }
    }

    logService.save(logBuilder.but()
                        .withLogLine("Command execution finished with status:" + CommandExecutionStatus.SUCCESS)
                        .withExecutionResult(CommandExecutionStatus.SUCCESS)
                        .build());

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);
    return anExecutionResponse()
        .withStateExecutionData(executionDataBuilder.build())
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  private void updateFunctionAlias(String region, String accessKey, char[] secretKey, String functionName,
      String functionArn, List<String> updateAlias, Builder logBuilder) {
    updateAlias.forEach(alias -> {
      logService.save(logBuilder.but().withLogLine("Updating Function Alias: " + alias).build());
      UpdateAliasResult updateAliasResult = awsHelperService.updateAlias(region, accessKey, secretKey,
          new UpdateAliasRequest().withFunctionName(functionName).withFunctionVersion(functionArn).withName(alias));
      logService.save(logBuilder.but()
                          .withLogLine(String.format("Updated Function Alias with name:[%s], arn:[%s]",
                              updateAliasResult.getName(), updateAliasResult.getAliasArn()))
                          .build());
    });
  }

  private void createFunctionAlias(String region, String accessKey, char[] secretKey, String functionName,
      String functionVersion, List<String> evaluatedAliases, Builder logBuilder) {
    evaluatedAliases.forEach(alias -> {
      logService.save(logBuilder.but().withLogLine("Creating Function Alias: " + alias).build());
      CreateAliasResult createAliasResult = awsHelperService.createAlias(region, accessKey, secretKey,
          new CreateAliasRequest().withFunctionName(functionName).withFunctionVersion(functionVersion).withName(alias));
      logService.save(logBuilder.but()
                          .withLogLine(String.format("Created Function Alias with name:[%s], arn:[%s]",
                              createAliasResult.getName(), createAliasResult.getAliasArn()))
                          .build());
    });
  }

  /**
   * Gets artifact.
   *
   * @param appId                  the app id
   * @param serviceId              the service id
   * @param workflowExecutionId    the workflow execution id
   * @param workflowStandardParams the workflow standard params
   * @return the artifact
   */
  protected Artifact getArtifact(
      String appId, String serviceId, String workflowExecutionId, WorkflowStandardParams workflowStandardParams) {
    return workflowStandardParams.getArtifactForService(serviceId);
  }

  private VpcConfig constructVpcConfig(AwsLambdaInfraStructureMapping infrastructureMapping) {
    String vpcId = infrastructureMapping.getVpcId();
    VpcConfig vpcConfig = new VpcConfig();
    if (vpcId != null) {
      List<String> subnetIds = infrastructureMapping.getSubnetIds();
      List<String> securityGroupIds = infrastructureMapping.getSecurityGroupIds();
      if (securityGroupIds.size() > 0 && subnetIds.size() > 0) {
        vpcConfig.setSubnetIds(subnetIds);
        vpcConfig.setSecurityGroupIds(securityGroupIds);
      } else {
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "At least one security group and one subnet must be provided");
      }
    }
    return vpcConfig;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  /**
   * Gets command name.
   *
   * @return the command name
   */
  public String getCommandName() {
    return commandName;
  }

  /**
   * Sets command name.
   *
   * @param commandName the command name
   */
  public void setCommandName(String commandName) {
    this.commandName = commandName;
  }

  /**
   * Gets aliases.
   *
   * @return the aliases
   */
  @SchemaIgnore
  public List<String> getAliases() {
    return aliases;
  }

  /**
   * Sets aliases.
   *
   * @param aliases the aliases
   */
  public void setAliases(List<String> aliases) {
    this.aliases = aliases;
  }
  //  /**
  //   * Gets role.
  //   *
  //   * @return the role
  //   */
  //  public String getRole() {
  //    return role;
  //  }
  //
  //  /**
  //   * Sets role.
  //   *
  //   * @param role the role
  //   */
  //  public void setRole(String role) {
  //    this.role = role;
  //  }
}
