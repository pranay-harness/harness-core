package software.wings.sm;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.joor.Reflect.on;
import static software.wings.beans.PhaseStepType.CLUSTER_SETUP;
import static software.wings.beans.PhaseStepType.CONTAINER_DEPLOY;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.DEPLOY_AWSCODEDEPLOY;
import static software.wings.beans.PhaseStepType.DEPLOY_SERVICE;
import static software.wings.beans.PhaseStepType.DISABLE_SERVICE;
import static software.wings.beans.PhaseStepType.ENABLE_SERVICE;
import static software.wings.beans.PhaseStepType.PROVISION_NODE;
import static software.wings.beans.PhaseStepType.SELECT_NODE;
import static software.wings.beans.PhaseStepType.START_SERVICE;
import static software.wings.beans.PhaseStepType.STOP_SERVICE;
import static software.wings.sm.StateTypeScope.COMMON;
import static software.wings.sm.StateTypeScope.NONE;
import static software.wings.sm.StateTypeScope.ORCHESTRATION_STENCILS;
import static software.wings.sm.StateTypeScope.PIPELINE_STENCILS;
import static software.wings.stencils.StencilCategory.CLOUD;
import static software.wings.stencils.StencilCategory.COMMANDS;
import static software.wings.stencils.StencilCategory.OTHERS;
import static software.wings.stencils.StencilCategory.VERIFICATIONS;

import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.io.Resources;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.PhaseStepType;
import software.wings.exception.WingsException;
import software.wings.sm.states.AppDynamicsState;
import software.wings.sm.states.ApprovalState;
import software.wings.sm.states.AwsAutoScaleProvisionState;
import software.wings.sm.states.AwsClusterSetup;
import software.wings.sm.states.AwsCodeDeployRollback;
import software.wings.sm.states.AwsCodeDeployState;
import software.wings.sm.states.AwsNodeSelectState;
import software.wings.sm.states.CloudWatchState;
import software.wings.sm.states.CommandState;
import software.wings.sm.states.DcNodeSelectState;
import software.wings.sm.states.EcsServiceDeploy;
import software.wings.sm.states.EcsServiceRollback;
import software.wings.sm.states.EcsServiceSetup;
import software.wings.sm.states.ElasticLoadBalancerState;
import software.wings.sm.states.ElkAnalysisState;
import software.wings.sm.states.EmailState;
import software.wings.sm.states.EnvState;
import software.wings.sm.states.ForkState;
import software.wings.sm.states.GcpClusterSetup;
import software.wings.sm.states.HttpState;
import software.wings.sm.states.JenkinsState;
import software.wings.sm.states.KubernetesReplicationControllerDeploy;
import software.wings.sm.states.KubernetesReplicationControllerRollback;
import software.wings.sm.states.KubernetesReplicationControllerSetup;
import software.wings.sm.states.PauseState;
import software.wings.sm.states.PhaseStepSubWorkflow;
import software.wings.sm.states.PhaseSubWorkflow;
import software.wings.sm.states.RepeatState;
import software.wings.sm.states.SplunkState;
import software.wings.sm.states.SplunkV2State;
import software.wings.sm.states.SubWorkflowState;
import software.wings.sm.states.WaitState;
import software.wings.stencils.OverridingStencil;
import software.wings.stencils.StencilCategory;
import software.wings.utils.JsonUtils;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents type of state.
 *
 * @author Rishi
 */
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum StateType implements StateTypeDescriptor {
  /**
   * Subworkflow state type.
   */
  SUB_WORKFLOW(SubWorkflowState.class, StencilCategory.CONTROLS, 0, asList(), ORCHESTRATION_STENCILS),

  /**
   * Repeat state type.
   */
  REPEAT(RepeatState.class, StencilCategory.CONTROLS, 1, asList(), ORCHESTRATION_STENCILS),

  /**
   * Fork state type.
   */
  FORK(ForkState.class, StencilCategory.CONTROLS, 2, asList(), ORCHESTRATION_STENCILS),

  /**
   * Wait state type.
   */
  WAIT(WaitState.class, StencilCategory.CONTROLS, 3, asList(), ORCHESTRATION_STENCILS),

  /**
   * Pause state type.
   */
  PAUSE(PauseState.class, StencilCategory.CONTROLS, 4, "Manual Step", asList(), ORCHESTRATION_STENCILS),

  /**
   * Http state type.
   */
  HTTP(HttpState.class, OTHERS, 1, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * App dynamics state type.
   */
  APP_DYNAMICS(AppDynamicsState.class, VERIFICATIONS, 2, asList(), ORCHESTRATION_STENCILS),

  /**
   * Splunk state type.
   */
  SPLUNK(SplunkState.class, VERIFICATIONS, 3, asList(), ORCHESTRATION_STENCILS),

  /**
   * Splunk V2 state type.
   */
  SPLUNKV2(SplunkV2State.class, VERIFICATIONS, 4, asList(), ORCHESTRATION_STENCILS),

  /**
   * Elk state type.
   */
  ELK(ElkAnalysisState.class, VERIFICATIONS, 5, "ELK", Collections.emptyList(), ORCHESTRATION_STENCILS),

  /**
   * Cloud watch state type.
   */
  CLOUD_WATCH(CloudWatchState.class, VERIFICATIONS, 6, asList(), ORCHESTRATION_STENCILS),

  /**
   * Email state type.
   */
  EMAIL(EmailState.class, StencilCategory.OTHERS, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * Env state state type.
   */
  ENV_STATE(EnvState.class, StencilCategory.ENVIRONMENTS, asList(), PIPELINE_STENCILS),

  /**
   * Command state type.
   */
  COMMAND(CommandState.class, StencilCategory.COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH, InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH),
      asList(START_SERVICE, STOP_SERVICE, DEPLOY_SERVICE, ENABLE_SERVICE, DISABLE_SERVICE), ORCHESTRATION_STENCILS),

  /**
   * Approval state type.
   */
  APPROVAL(ApprovalState.class, StencilCategory.OTHERS, asList(), PIPELINE_STENCILS, COMMON),

  /**
   * The Load balancer.
   */
  ELASTIC_LOAD_BALANCER(ElasticLoadBalancerState.class, StencilCategory.COMMANDS, "Elastic Load Balancer",
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH, InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH),
      asList(ENABLE_SERVICE, DISABLE_SERVICE), ORCHESTRATION_STENCILS),

  /**
   * Jenkins state type.
   */
  JENKINS(JenkinsState.class, OTHERS, asList(), ORCHESTRATION_STENCILS, COMMON),

  /**
   * AWS Node Select state.
   */
  AWS_NODE_SELECT(AwsNodeSelectState.class, CLOUD, Lists.newArrayList(InfrastructureMappingType.AWS_SSH),
      asList(PROVISION_NODE, SELECT_NODE), ORCHESTRATION_STENCILS),

  /**
   * AWS Node Provision state.
   */
  AWS_AUTOSCALE_PROVISION(AwsAutoScaleProvisionState.class, CLOUD,
      Lists.newArrayList(InfrastructureMappingType.AWS_SSH), asList(PROVISION_NODE, SELECT_NODE),
      ORCHESTRATION_STENCILS),

  /**
   * Phase state type.
   */
  DC_NODE_SELECT(DcNodeSelectState.class, CLOUD, Lists.newArrayList(InfrastructureMappingType.PHYSICAL_DATA_CENTER_SSH),
      asList(SELECT_NODE), ORCHESTRATION_STENCILS),

  /**
   * Phase state type.
   */
  PHASE(PhaseSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  /**
   * Phase state type.
   */
  PHASE_STEP(PhaseStepSubWorkflow.class, StencilCategory.SUB_WORKFLOW, asList(), NONE),

  AWS_CODEDEPLOY_STATE(AwsCodeDeployState.class, COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_CODEDEPLOY), asList(DEPLOY_AWSCODEDEPLOY),
      ORCHESTRATION_STENCILS),

  AWS_CODEDEPLOY_ROLLBACK(AwsCodeDeployRollback.class, COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.AWS_AWS_CODEDEPLOY), asList(DEPLOY_AWSCODEDEPLOY),
      ORCHESTRATION_STENCILS),

  ECS_SERVICE_SETUP(EcsServiceSetup.class, CLOUD, Lists.newArrayList(InfrastructureMappingType.AWS_ECS),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  ECS_SERVICE_DEPLOY(EcsServiceDeploy.class, COMMANDS, Lists.newArrayList(InfrastructureMappingType.AWS_ECS),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  ECS_SERVICE_ROLLBACK(EcsServiceRollback.class, COMMANDS, Lists.newArrayList(InfrastructureMappingType.AWS_ECS),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  KUBERNETES_REPLICATION_CONTROLLER_SETUP(KubernetesReplicationControllerSetup.class, CLOUD,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_SETUP), ORCHESTRATION_STENCILS),

  KUBERNETES_REPLICATION_CONTROLLER_DEPLOY(KubernetesReplicationControllerDeploy.class, COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  KUBERNETES_REPLICATION_CONTROLLER_ROLLBACK(KubernetesReplicationControllerRollback.class, COMMANDS,
      Lists.newArrayList(InfrastructureMappingType.DIRECT_KUBERNETES, InfrastructureMappingType.GCP_KUBERNETES),
      asList(CONTAINER_DEPLOY), ORCHESTRATION_STENCILS),

  AWS_CLUSTER_SETUP(AwsClusterSetup.class, CLOUD, Lists.newArrayList(InfrastructureMappingType.AWS_ECS),
      asList(CLUSTER_SETUP), ORCHESTRATION_STENCILS),

  GCP_CLUSTER_SETUP(GcpClusterSetup.class, CLOUD, Lists.newArrayList(InfrastructureMappingType.GCP_KUBERNETES),
      asList(CLUSTER_SETUP), ORCHESTRATION_STENCILS);

  private static final String stencilsPath = "/templates/stencils/";
  private static final String uiSchemaSuffix = "-UISchema.json";
  private final Logger logger = LoggerFactory.getLogger(getClass());
  private Class<? extends State> stateClass;
  private Object jsonSchema;
  private Object uiSchema;
  private List<StateTypeScope> scopes = new ArrayList<>();
  private List<String> phaseStepTypes = new ArrayList<>();
  private StencilCategory stencilCategory;
  private Integer displayOrder = DEFAULT_DISPLAY_ORDER;
  private String displayName = UPPER_UNDERSCORE.to(UPPER_CAMEL, name());
  private List<InfrastructureMappingType> supportedInfrastructureMappingTypes = Collections.emptyList();

  /**
   * Instantiates a new state type.
   *
   * @param stateClass the state class
   * @param scopes     the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, phaseStepTypes, scopes);
  }

  <E> StateType(Class<? extends State> stateClass, StencilCategory stencilCategory,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, supportedInfrastructureMappingTypes, phaseStepTypes,
        scopes);
  }

  /**
   * Instantiates a new state type.
   *
   * @param stateClass   the state class
   * @param displayOrder display order
   * @param scopes       the scopes
   */
  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, Collections.emptyList(), phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, null, supportedInfrastructureMappingTypes, phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, String displayName,
      List<InfrastructureMappingType> supportedInfrastructureMappingTypes, List<PhaseStepType> phaseStepTypes,
      StateTypeScope... scopes) {
    this(stateClass, stencilCategory, DEFAULT_DISPLAY_ORDER, displayName, supportedInfrastructureMappingTypes,
        phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      String displayName, List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this(stateClass, stencilCategory, displayOrder, displayName, Collections.emptyList(), phaseStepTypes, scopes);
  }

  StateType(Class<? extends State> stateClass, StencilCategory stencilCategory, Integer displayOrder,
      String displayName, List<InfrastructureMappingType> supportedInfrastructureMappingTypes,
      List<PhaseStepType> phaseStepTypes, StateTypeScope... scopes) {
    this.stateClass = stateClass;
    this.scopes = asList(scopes);
    this.phaseStepTypes =
        phaseStepTypes.stream().map(phaseStepType -> phaseStepType.name()).collect(Collectors.toList());
    this.jsonSchema = loadJsonSchema();
    this.stencilCategory = stencilCategory;
    this.displayOrder = displayOrder;
    if (isNotBlank(displayName)) {
      this.displayName = displayName;
    }
    try {
      this.uiSchema = readResource(stencilsPath + name() + uiSchemaSuffix);
    } catch (Exception e) {
      this.uiSchema = new HashMap<String, Object>();
    }
    this.supportedInfrastructureMappingTypes = supportedInfrastructureMappingTypes;
  }

  private Object readResource(String file) {
    try {
      URL url = this.getClass().getResource(file);
      String json = Resources.toString(url, Charsets.UTF_8);
      return JsonUtils.asObject(json, HashMap.class);
    } catch (Exception exception) {
      throw new WingsException("Error in initializing StateType-" + file, exception);
    }
  }

  @Override
  @JsonValue
  public String getType() {
    return name();
  }

  @Override
  public Class<? extends State> getTypeClass() {
    return stateClass;
  }

  @Override
  public JsonNode getJsonSchema() {
    return ((JsonNode) jsonSchema).deepCopy();
  }

  @Override
  public Object getUiSchema() {
    return uiSchema;
  }

  @Override
  public List<StateTypeScope> getScopes() {
    return scopes;
  }

  @Override
  public String getName() {
    return displayName;
  }

  /*
   * (non-Javadoc)
   *
   * @see software.wings.sm.StateTypeDescriptor#newInstance(java.lang.String)
   */
  @Override
  public State newInstance(String id) {
    return on(stateClass).create(id).get();
  }

  @Override
  public OverridingStencil getOverridingStencil() {
    return new OverridingStateTypeDescriptor(this);
  }

  private JsonNode loadJsonSchema() {
    return JsonUtils.jsonSchema(stateClass);
  }

  @Override
  public StencilCategory getStencilCategory() {
    return stencilCategory;
  }

  @Override
  public Integer getDisplayOrder() {
    return displayOrder;
  }

  @Override
  public boolean matches(Object context) {
    InfrastructureMapping infrastructureMapping = (InfrastructureMapping) context;
    InfrastructureMappingType infrastructureMappingType =
        InfrastructureMappingType.valueOf(infrastructureMapping.getInfraMappingType());
    return (stencilCategory != COMMANDS && stencilCategory != CLOUD)
        || supportedInfrastructureMappingTypes.contains(infrastructureMappingType);
  }

  @Override
  public List<String> getPhaseStepTypes() {
    return phaseStepTypes;
  }
}
