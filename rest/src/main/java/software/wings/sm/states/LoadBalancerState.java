package software.wings.sm.states;

import static java.util.Optional.ofNullable;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.api.ElbStateExecutionData.Builder.anElbStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import ro.fortsoft.pf4j.PluginManager;
import software.wings.api.InstanceElement;
import software.wings.api.LoadBalancer;
import software.wings.api.LoadBalancerConfig;
import software.wings.beans.ServiceVariable;
import software.wings.beans.ServiceVariable.Type;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.WorkflowStandardParams;
import software.wings.stencils.DefaultValue;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public class LoadBalancerState extends State {
  @Transient @Inject private transient ServiceTemplateService serviceTemplateService;

  @Transient @Inject private transient SettingsService settingsService;

  @Transient @Inject private Set<LoadBalancer> systemLoadBalancers;

  @Transient @Inject private PluginManager pluginManager;

  @Attributes(title = "Operation") private Operation operation;

  @DefaultValue("Instance") @Attributes(title = "Entity") private Entity entity;

  @Attributes(title = "Custom Entity") private String custom;

  public LoadBalancerState(String name) {
    super(name, StateType.LOAD_BALANCER.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionStatus status = ExecutionStatus.SUCCESS;
    ContextElement contextElement =
        (ContextElement) context.evaluateExpression(isNotBlank(custom) ? custom : entity.getExpression());
    String errorMessage = "";
    WorkflowStandardParams standardParam = context.getContextElement(ContextElementType.STANDARD);
    InstanceElement instance = context.getContextElement(ContextElementType.INSTANCE);
    Optional<ServiceVariable> serviceVariableOpt =
        ofNullable(serviceTemplateService
                       .computeServiceVariables(standardParam.getAppId(), standardParam.getEnvId(),
                           instance.getServiceTemplateElement().getUuid(), instance.getHostElement().getUuid())
                       .stream()
                       .filter(serviceVariable -> serviceVariable.getType() == Type.LB)
                       .findFirst()
                       .orElse(null));

    if (serviceVariableOpt.isPresent()) {
      SettingAttribute settingAttribute =
          settingsService.get(standardParam.getAppId(), serviceVariableOpt.get().getValue());
      LoadBalancerConfig loadBalancerConfig = (LoadBalancerConfig) settingAttribute.getValue();
      LoadBalancer<LoadBalancerConfig> loadBalancer = getLoadBalancer(loadBalancerConfig);
      if (loadBalancer != null) {
        try {
          boolean result = operation == Operation.Enable
              ? loadBalancer.enableInstance(loadBalancerConfig, contextElement)
              : loadBalancer.disableInstance(loadBalancerConfig, contextElement);
          status = result ? ExecutionStatus.SUCCESS : ExecutionStatus.FAILED;
        } catch (Exception e) {
          status = ExecutionStatus.ERROR;
          errorMessage = e.getMessage();
        }
      }
    }

    return anExecutionResponse()
        .withStateExecutionData(anElbStateExecutionData().withHostName(instance.getHostElement().getHostName()).build())
        .withExecutionStatus(status)
        .withErrorMessage(errorMessage)
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  private LoadBalancer getLoadBalancer(LoadBalancerConfig loadBalancerConfig) {
    List<LoadBalancer> loadBalancers = pluginManager.getExtensions(LoadBalancer.class);
    return systemLoadBalancers.stream()
        .filter(loadBalancer -> loadBalancerConfig.getClass().isAssignableFrom(loadBalancer.supportedConfig()))
        .findFirst()
        .orElse(
            loadBalancers.stream()
                .filter(loadBalancer -> loadBalancerConfig.getClass().isAssignableFrom(loadBalancer.supportedConfig()))
                .findFirst()
                .orElse(null));
  }

  /**
   * Getter for property 'operation'.
   *
   * @return Value for property 'operation'.
   */
  public Operation getOperation() {
    return operation;
  }

  /**
   * Setter for property 'operation'.
   *
   * @param operation Value to set for property 'operation'.
   */
  public void setOperation(Operation operation) {
    this.operation = operation;
  }

  public String getCustom() {
    return custom;
  }

  public void setCustom(String custom) {
    this.custom = custom;
  }

  /**
   * Getter for property 'entity'.
   *
   * @return Value for property 'entity'.
   */
  public Entity getEntity() {
    return entity;
  }

  /**
   * Setter for property 'entity'.
   *
   * @param entity Value to set for property 'entity'.
   */
  public void setEntity(Entity entity) {
    this.entity = entity;
  }

  public enum Operation { Enable, Disable }

  private enum Entity {
    Instance("${instance}"),
    Custom("");

    private String expression;

    Entity(String expression) {
      this.expression = expression;
    }

    public String getExpression() {
      return expression;
    }
  }
}
