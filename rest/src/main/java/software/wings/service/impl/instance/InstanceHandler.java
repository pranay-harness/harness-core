package software.wings.service.impl.instance;

import com.google.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.DeploymentInfo;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.HarnessException;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TriggerService;
import software.wings.service.intfc.instance.InstanceService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

/**
 * @author rktummala on 01/30/18
 */
public abstract class InstanceHandler {
  protected static final Logger logger = LoggerFactory.getLogger(InstanceHandler.class);

  @Inject protected InstanceHelper instanceHelper;
  @Inject protected InstanceService instanceService;
  @Inject protected InfrastructureMappingService infraMappingService;
  @Inject protected SettingsService settingsService;
  @Inject protected SecretManager secretManager;
  @Inject protected TriggerService triggerService;
  public static final String AUTO_SCALE = "AUTO_SCALE";

  public abstract void syncInstances(String appId, String infraMappingId) throws HarnessException;
  public abstract void handleNewDeployment(DeploymentInfo deploymentInfo) throws HarnessException;

  protected List<Instance> getInstances(String appId, String infraMappingId) {
    PageRequest<Instance> pageRequest = new PageRequest<>();
    pageRequest.addFilter("infraMappingId", Operator.EQ, infraMappingId);
    pageRequest.addFilter("appId", Operator.EQ, appId);
    PageResponse<Instance> pageResponse = instanceService.list(pageRequest);
    return pageResponse.getResponse();
  }
}
