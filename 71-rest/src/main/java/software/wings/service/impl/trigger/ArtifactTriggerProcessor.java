package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ServiceResourceService;

@Singleton
public class ArtifactTriggerProcessor implements TriggerProcessor {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ServiceResourceService serviceResourceService;

  @Override
  public void validateTriggerCondition(DeploymentTrigger trigger) {
    ArtifactCondition artifactCondition = (ArtifactCondition) trigger.getCondition();

    ArtifactStream artifactStream =
        artifactStreamService.get(trigger.getAppId(), artifactCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);

    String serviceName = serviceResourceService.fetchServiceName(trigger.getAppId(), artifactStream.getServiceId());
    notNullCheck("Service associated to the artifact source [" + artifactStream.getSourceName() + "] does not exist",
        serviceName, USER);
    artifactCondition.setArtifactSourceName(artifactStream.getSourceName() + " (" + serviceName + ")");
  }
}