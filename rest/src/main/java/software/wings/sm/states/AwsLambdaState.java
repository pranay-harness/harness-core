package software.wings.sm.states;

import static software.wings.api.CommandStateExecutionData.Builder.aCommandStateExecutionData;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.common.Constants.AWS_LAMBDA;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.google.inject.Inject;

import com.amazonaws.services.lambda.model.CreateFunctionRequest;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionCode;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeRequest;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
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
import software.wings.beans.Log.LogLevel;
import software.wings.beans.Service;
import software.wings.beans.SettingAttribute;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.Command;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.cloudprovider.aws.AwsLambdaService;
import software.wings.common.Constants;
import software.wings.exception.WingsException;
import software.wings.service.impl.AwsHelperService;
import software.wings.service.intfc.ActivityService;
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
import software.wings.stencils.DataProvider;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

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

  @Attributes(title = "Command")
  @EnumData(enumDataProvider = CommandStateEnumDataProvider.class)
  @DefaultValue(AWS_LAMBDA)
  @SchemaIgnore
  private String commandName = AWS_LAMBDA;

  @Attributes(title = "S3 Bucket", required = true) private String bucket;
  @Attributes(title = "Key", required = true) private String key;
  @EnumData(enumDataProvider = LambdaRuntimeProvider.class)
  @DefaultValue("nodejs4.3")
  @Attributes(title = "Runtime", required = true)
  private String runtime;
  @Attributes(title = "Memory Size", required = true)
  @EnumData(enumDataProvider = LambdaMemorySizePrvider.class)
  @DefaultValue("128")
  private Integer memorySize;
  @Attributes(title = "Execution Timeout", required = true) @DefaultValue("3") private Integer timeout;
  @Attributes(title = "Function Name", required = true) private String functionName;
  @Attributes(title = "Handler", required = true) private String handler;
  @Attributes(title = "Role", required = true) private String role;

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

    AwsLambdaInfraStructureMapping infrastructureMapping =
        (AwsLambdaInfraStructureMapping) infrastructureMappingService.get(
            app.getUuid(), phaseElement.getInfraMappingId());

    SettingAttribute cloudProviderSetting = settingsService.get(infrastructureMapping.getComputeProviderSettingId());
    String region = infrastructureMapping.getRegion();

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
                                           .withCommandUnits(serviceResourceService.getFlattenCommandUnitList(
                                               app.getUuid(), serviceId, envId, command.getName()))
                                           .withCommandType(command.getCommandUnitType().name())
                                           .withServiceVariables(context.getServiceVariables());

    Activity activity = activityService.save(activityBuilder.build());

    logService.save(aLog()
                        .withAppId(activity.getAppId())
                        .withActivityId(activity.getUuid())
                        .withLogLevel(LogLevel.INFO)
                        .withCommandUnitName(commandName)
                        .withLogLine("Begin command execution.")
                        .withExecutionResult(CommandExecutionStatus.RUNNING)
                        .build());

    CommandStateExecutionData.Builder executionDataBuilder = aCommandStateExecutionData()
                                                                 .withServiceId(service.getUuid())
                                                                 .withServiceName(service.getName())
                                                                 .withAppId(app.getUuid())
                                                                 .withCommandName(getCommandName())
                                                                 .withActivityId(activity.getUuid());

    String key = context.renderExpression(this.key);
    String bucket = context.renderExpression(this.bucket);

    logService.save(aLog()
                        .withAppId(activity.getAppId())
                        .withActivityId(activity.getUuid())
                        .withLogLevel(LogLevel.INFO)
                        .withCommandUnitName(commandName)
                        .withLogLine("Begin command execution.")
                        .withExecutionResult(CommandExecutionStatus.RUNNING)
                        .build());

    AwsConfig value = (AwsConfig) cloudProviderSetting.getValue();

    Map<String, String> serviceVariables =
        serviceTemplateService
            .computeServiceVariables(app.getUuid(), envId, infrastructureMapping.getServiceTemplateId())
            .stream()
            .collect(Collectors.toMap(sv -> sv.getName(), sv -> context.renderExpression(new String(sv.getValue()))));

    Artifact artifact = workflowStandardParams.getArtifactForService(serviceId);
    if (artifact == null) {
      throw new StateExecutionException(String.format("Unable to find artifact for service %s", service.getName()));
    }

    GetFunctionResult getFunctionResult = awsHelperService.getFunction(
        region, value.getAccessKey(), value.getSecretKey(), new GetFunctionRequest().withFunctionName(functionName));

    if (getFunctionResult == null) { // function doesn't exist
      logService.save(aLog()
                          .withAppId(activity.getAppId())
                          .withActivityId(activity.getUuid())
                          .withLogLevel(LogLevel.INFO)
                          .withCommandUnitName(commandName)
                          .withLogLine("Function doesn't exist. Create and Publish")
                          .withExecutionResult(CommandExecutionStatus.RUNNING)
                          .build());
      // create function

      CreateFunctionRequest createFunctionRequest =
          new CreateFunctionRequest()
              .withEnvironment(new com.amazonaws.services.lambda.model.Environment().withVariables(serviceVariables))
              .withRuntime(runtime)
              .withFunctionName(functionName)
              .withHandler(handler)
              .withRole(role)
              .withCode(new FunctionCode().withS3Bucket(bucket).withS3Key(key))
              .withPublish(true);

      VpcConfig vpcConfig = constructVpcConfig(infrastructureMapping);

      if (vpcConfig != null) {
        createFunctionRequest.setVpcConfig(vpcConfig);
      }
      CreateFunctionResult function =
          awsHelperService.createFunction(region, value.getAccessKey(), value.getSecretKey(), createFunctionRequest);
      System.out.println(function.toString());
    } else {
      logService.save(aLog()
                          .withAppId(activity.getAppId())
                          .withActivityId(activity.getUuid())
                          .withLogLevel(LogLevel.INFO)
                          .withCommandUnitName(commandName)
                          .withLogLine("Function exists. Update and Publish")
                          .withExecutionResult(CommandExecutionStatus.RUNNING)
                          .build());
      UpdateFunctionCodeResult updateFunctionCodeResult =
          awsHelperService.updateFunction(region, value.getAccessKey(), value.getSecretKey(),
              new UpdateFunctionCodeRequest()
                  .withFunctionName(functionName)
                  .withPublish(true)
                  .withS3Bucket(bucket)
                  .withS3Key(key));
    }

    //    ExecutionStatus status = commandExecutionResult != null &&
    //    CommandExecutionStatus.SUCCESS.equals(commandExecutionResult.getStatus()) ?
    //        ExecutionStatus.SUCCESS :
    //        ExecutionStatus.FAILED;

    logService.save(aLog()
                        .withAppId(activity.getAppId())
                        .withActivityId(activity.getUuid())
                        .withLogLevel(LogLevel.INFO)
                        .withCommandUnitName(commandName)
                        .withLogLine("Command execution finished with status:" + CommandExecutionStatus.SUCCESS)
                        .withExecutionResult(CommandExecutionStatus.SUCCESS)
                        .build());

    activityService.updateStatus(activity.getUuid(), activity.getAppId(), ExecutionStatus.SUCCESS);
    return anExecutionResponse()
        .withStateExecutionData(executionDataBuilder.build())
        .withExecutionStatus(ExecutionStatus.SUCCESS)
        .build();
  }

  public VpcConfig constructVpcConfig(AwsLambdaInfraStructureMapping infrastructureMapping) {
    String vpcId = infrastructureMapping.getVpcId();
    VpcConfig vpcConfig = null;
    if (vpcId != null) {
      List<String> subnetIds = infrastructureMapping.getSubnetIds();
      List<String> securityGroupIds = infrastructureMapping.getSecurityGroupIds();
      if (securityGroupIds.size() > 0 && subnetIds.size() > 0) {
        vpcConfig = new VpcConfig().withSecurityGroupIds(securityGroupIds).withSubnetIds(subnetIds);
      } else {
        throw new WingsException(
            ErrorCode.INVALID_REQUEST, "message", "Atleast one sercurity group and one subnet must be provided");
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
  @SchemaIgnore
  public String getCommandName() {
    return commandName;
  }

  /**
   * Gets bucket.
   *
   * @return the bucket
   */
  public String getBucket() {
    return bucket;
  }

  /**
   * Sets bucket.
   *
   * @param bucket the bucket
   */
  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  /**
   * Gets key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Sets key.
   *
   * @param key the key
   */
  public void setKey(String key) {
    this.key = key;
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
   * Gets runtime.
   *
   * @return the runtime
   */
  public String getRuntime() {
    return runtime;
  }

  /**
   * Sets runtime.
   *
   * @param runtime the runtime
   */
  public void setRuntime(String runtime) {
    this.runtime = runtime;
  }

  /**
   * Gets function name.
   *
   * @return the function name
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Sets function name.
   *
   * @param functionName the function name
   */
  public void setFunctionName(String functionName) {
    this.functionName = functionName;
  }

  /**
   * Gets handler.
   *
   * @return the handler
   */
  public String getHandler() {
    return handler;
  }

  /**
   * Sets handler.
   *
   * @param handler the handler
   */
  public void setHandler(String handler) {
    this.handler = handler;
  }

  /**
   * Gets role.
   *
   * @return the role
   */
  public String getRole() {
    return role;
  }

  /**
   * Sets role.
   *
   * @param role the role
   */
  public void setRole(String role) {
    this.role = role;
  }

  public Integer getMemorySize() {
    return memorySize;
  }

  public void setMemorySize(Integer memorySize) {
    this.memorySize = memorySize;
  }

  public Integer getTimeout() {
    return timeout;
  }

  public void setTimeout(Integer timeout) {
    this.timeout = timeout;
  }

  /**
   * The type Lambda runtime provider.
   */
  public static class LambdaRuntimeProvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return Stream.of("nodejs4.3", "nodejs4.3-edge", "nodejs6.10", "python2.7", "python3.6", "java8", "dotnetcore1.0")
          .collect(Collectors.toMap(o -> o, o -> o, (o1, o2) -> o1, LinkedHashMap::new));
    }
  }

  public static class LambdaMemorySizePrvider implements DataProvider {
    @Override
    public Map<String, String> getData(String appId, String... params) {
      return IntStream.range(2, 48)
          .mapToObj(m -> String.valueOf(m * 64))
          .collect(Collectors.toMap(v -> v, v -> v + " MB", (p, q) -> p, LinkedHashMap::new));
    }
  }
}
