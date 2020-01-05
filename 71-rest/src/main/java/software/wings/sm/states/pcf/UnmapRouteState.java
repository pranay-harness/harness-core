package software.wings.sm.states.pcf;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.github.reinert.jjschema.Attributes;
import software.wings.common.Constants;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class UnmapRouteState extends MapRouteState {
  @DefaultValue("${" + Constants.PCF_APP_NAME + "}") @Attributes(title = "PCF App Name") private String pcfAppName;
  @DefaultValue("${" + WorkflowServiceHelper.INFRA_ROUTE_PCF + "}")
  @Attributes(title = "Map Route")
  private String route;

  @Override
  public String getPcfAppName() {
    return pcfAppName;
  }

  @Override
  public void setPcfAppName(String pcfAppName) {
    this.pcfAppName = pcfAppName;
  }

  @Override
  public String getRoute() {
    return route;
  }

  @Override
  public void setRoute(String route) {
    this.route = route;
  }

  /**
   * Instantiates a new state.
   *
   * @param name      the name
   */
  public UnmapRouteState(String name) {
    super(name, StateType.PCF_UNMAP_ROUTE.name());
  }

  public UnmapRouteState(String name, String stateType) {
    super(name, stateType);
  }

  @Override
  public boolean checkIfMapRouteOperation() {
    return false;
  }
}
