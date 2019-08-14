package software.wings.service.intfc.trigger;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import io.harness.validation.Update;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.trigger.DeploymentTrigger;

import java.util.List;
import javax.validation.Valid;

public interface DeploymentTriggerService {
  @ValidationGroups(Create.class) DeploymentTrigger save(@Valid DeploymentTrigger deploymentTrigger);

  @ValidationGroups(Update.class) DeploymentTrigger update(@Valid DeploymentTrigger trigger);

  void delete(@NotEmpty String appId, @NotEmpty String triggerId);

  DeploymentTrigger get(@NotEmpty String appId, @NotEmpty String triggerId);

  PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest);

  void triggerExecutionPostArtifactCollectionAsync(
      String accountId, String appId, String artifactStreamId, List<Artifact> artifacts);

  /**
   * Gets the cron expression
   *
   * @param expression
   * @return
   */
  String getCronDescription(String expression);

  void triggerScheduledExecutionAsync(DeploymentTrigger trigger);

  void triggerExecutionPostPipelineCompletionAsync(String appId, String pipelineId);
}