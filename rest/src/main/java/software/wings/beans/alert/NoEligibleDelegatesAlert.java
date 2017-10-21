package software.wings.beans.alert;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskGroup;
import software.wings.service.intfc.EnvironmentService;

import java.util.Objects;
import javax.inject.Inject;

@Data
@Builder
public class NoEligibleDelegatesAlert implements AlertData {
  @Inject @Transient @SchemaIgnore private transient EnvironmentService environmentService;

  private DelegateTask task;
  private TaskGroup taskType;

  @Override
  public boolean matches(AlertData alertData) {
    NoEligibleDelegatesAlert otherAlertData = (NoEligibleDelegatesAlert) alertData;
    DelegateTask otherTask = otherAlertData.getTask();

    boolean match = taskType == otherAlertData.getTaskType() && Objects.equals(task.getAppId(), otherTask.getAppId())
        && Objects.equals(task.getEnvId(), otherTask.getEnvId())
        && Objects.equals(task.getInfrastructureMappingId(), otherTask.getInfrastructureMappingId());

    if (match && task.getAppId() != null && task.getEnvId() != null) {
      match = environmentService.get(task.getAppId(), task.getEnvId(), false).getEnvironmentType()
          == environmentService.get(otherTask.getAppId(), otherTask.getEnvId(), false).getEnvironmentType();
    }

    return match;
  }
}
