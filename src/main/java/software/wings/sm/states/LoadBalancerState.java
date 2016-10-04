package software.wings.sm.states;

import static java.util.Optional.ofNullable;
import static software.wings.api.ElbStateExecutionData.Builder.anElbStateExecutionData;
import static software.wings.sm.ExecutionResponse.Builder.anExecutionResponse;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.State;
import software.wings.sm.WorkflowStandardParams;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 10/3/16.
 */
public abstract class LoadBalancerState extends State {
  @Transient @Inject public transient ServiceTemplateService serviceTemplateService;

  @Transient @Inject public transient SettingsService settingsService;

  @Transient @Inject private Set<LoadBalancer> systemLoadBalancers;

  @Transient @Inject private PluginManager pluginManager;

  @JsonIgnore private boolean enable;

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   * @param stateType the state type
   */
  public LoadBalancerState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    ExecutionStatus status = ExecutionStatus.SUCCESS;
    String errorMessage = "";
    WorkflowStandardParams standardParam = context.getContextElement(ContextElementType.STANDARD);
    InstanceElement instance = context.getContextElement(ContextElementType.INSTANCE);
    Optional<ServiceVariable> serviceVariableOpt =
        ofNullable(serviceTemplateService
                       .computeServiceVariables(standardParam.getAppId(), standardParam.getEnvId(),
                           instance.getServiceTemplateElement().getUuid())
                       .get(instance.getHostElement().getUuid()))
            .orElse(Collections.emptyList())
            .stream()
            .filter(serviceVariable -> serviceVariable.getType() == Type.LB)
            .findFirst();

    if (serviceVariableOpt.isPresent()) {
      SettingAttribute settingAttribute =
          settingsService.get(standardParam.getAppId(), serviceVariableOpt.get().getValue());
      LoadBalancerConfig loadBalancerConfig = (LoadBalancerConfig) settingAttribute.getValue();
      LoadBalancer<LoadBalancerConfig> loadBalancer = getLoadBalancer(loadBalancerConfig);
      if (loadBalancer != null) {
        try {
          boolean result = enable ? loadBalancer.enableHost(loadBalancerConfig, instance.getHostElement())
                                  : loadBalancer.disableHost(loadBalancerConfig, instance.getHostElement());
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

  public void setEnable(boolean enable) {
    this.enable = enable;
  }
}
