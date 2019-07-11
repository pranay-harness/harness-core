package software.wings.service.impl.trigger;

import static io.harness.exception.WingsException.USER;
import static software.wings.utils.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.ArtifactCondition;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.ArtifactStreamServiceBindingService;
import software.wings.service.intfc.ServiceResourceService;

@Singleton
public class ArtifactTriggerProcessor implements TriggerProcessor {
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactStreamServiceBindingService artifactStreamServiceBindingService;
  @Inject ServiceResourceService serviceResourceService;

  @Override
  public void validateTriggerCondition(DeploymentTrigger trigger) {
    // TODO: ASR: update when index added on setting_id + name
    ArtifactCondition artifactCondition = (ArtifactCondition) trigger.getCondition();

    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());
    notNullCheck("Artifact Source is mandatory for New Artifact Condition Trigger", artifactStream, USER);

    Service service =
        artifactStreamServiceBindingService.getService(trigger.getAppId(), artifactStream.getUuid(), false);
    notNullCheck("Service associated to the artifact source [" + artifactStream.getSourceName() + "] does not exist",
        service, USER);

    trigger.setCondition(ArtifactCondition.builder()
                             .artifactStreamId(artifactCondition.getArtifactStreamId())
                             .artifactSourceName(artifactStream.getSourceName())
                             .artifactFilter(artifactCondition.getArtifactFilter())
                             .build());
  }

  @Override
  public void updateTriggerCondition(DeploymentTrigger deploymentTrigger) {
    ArtifactCondition artifactCondition = (ArtifactCondition) deploymentTrigger.getCondition();
    ArtifactStream artifactStream = artifactStreamService.get(artifactCondition.getArtifactStreamId());

    deploymentTrigger.setCondition(ArtifactCondition.builder()
                                       .artifactStreamId(artifactCondition.getArtifactStreamId())
                                       .artifactSourceName(artifactStream.getSourceName())
                                       .artifactStreamType(artifactStream.getArtifactStreamType())
                                       .artifactFilter(artifactCondition.getArtifactFilter())
                                       .build());
  }
}