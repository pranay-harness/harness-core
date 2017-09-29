package software.wings.sm.states;

import com.google.inject.Inject;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ResizeStrategy;
import software.wings.helpers.ext.ecr.EcrClassicService;
import software.wings.helpers.ext.ecr.EcrService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.State;

/**
 * Created by brett on 9/29/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class ContainerServiceSetup extends State {
  protected static final int KEEP_N_REVISIONS = 3;

  private int maxInstances;
  private ResizeStrategy resizeStrategy;
  @Inject @Transient protected transient EcrService ecrService;
  @Inject @Transient protected transient EcrClassicService ecrClassicService;
  @Inject @Transient protected transient SettingsService settingsService;
  @Inject @Transient protected transient ServiceResourceService serviceResourceService;
  @Inject @Transient protected transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient protected transient ArtifactStreamService artifactStreamService;

  public ContainerServiceSetup(String name, String type) {
    super(name, type);
  }

  public int getMaxInstances() {
    return maxInstances;
  }

  public void setMaxInstances(int maxInstances) {
    this.maxInstances = maxInstances;
  }

  public ResizeStrategy getResizeStrategy() {
    return resizeStrategy;
  }

  public void setResizeStrategy(ResizeStrategy resizeStrategy) {
    this.resizeStrategy = resizeStrategy;
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}
}
