package software.wings.service.intfc.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.validation.Create;
import org.hibernate.validator.constraints.NotEmpty;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;
import software.wings.beans.trigger.TriggerExecution;
import software.wings.beans.trigger.TriggerExecution.Status;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
public interface TriggerExecutionService {
  @ValidationGroups(Create.class) TriggerExecution save(@Valid TriggerExecution triggerExecution);

  TriggerExecution get(@NotEmpty String appId, @NotEmpty String triggerId);

  PageResponse<TriggerExecution> list(PageRequest<TriggerExecution> pageRequest);

  TriggerExecution fetchLastSuccessOrRunningExecution(
      @NotEmpty String appId, @NotEmpty String triggerId, @NotEmpty String webhookToken);

  void updateStatus(String appId, String triggerExecutionId, @NotNull Status status, String errorMsg);
}
