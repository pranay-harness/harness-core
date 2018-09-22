package software.wings.sm.states.pcf;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.mongodb.morphia.annotations.Transient;
import software.wings.common.Constants;
import software.wings.service.intfc.LogService;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

@SuppressFBWarnings("MF_CLASS_MASKS_FIELD")
public class UnmapRouteState extends MapRouteState {
  @Inject @Transient protected transient LogService logService;

  @DefaultValue("${" + Constants.PCF_APP_NAME + "}") @Attributes(title = "PCF App Name") private String pcfAppName;
  @DefaultValue("${" + Constants.INFRA_ROUTE_PCF + "}") @Attributes(title = "Map Route") private String route;

  public String getPcfAppName() {
    return pcfAppName;
  }

  public void setPcfAppName(String pcfAppName) {
    this.pcfAppName = pcfAppName;
  }

  public String getRoute() {
    return route;
  }

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
