package software.wings.service.intfc.trigger;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.trigger.DeploymentTrigger;

import javax.validation.Valid;

public interface DeploymentTriggerService {
  @ValidationGroups(Create.class) DeploymentTrigger save(@Valid DeploymentTrigger deploymentTrigger);

  @ValidationGroups(Create.class) DeploymentTrigger update(@Valid DeploymentTrigger trigger);

  void delete(@NotEmpty String appId, @NotEmpty String triggerId);

  DeploymentTrigger get(@NotEmpty String appId, @NotEmpty String triggerId);

  PageResponse<DeploymentTrigger> list(PageRequest<DeploymentTrigger> pageRequest);
}