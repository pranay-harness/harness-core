package software.wings.beans.alert;

import com.github.reinert.jjschema.SchemaIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.CatalogItem;
import software.wings.beans.DelegateTask;
import software.wings.beans.TaskGroup;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.inject.Inject;

public class NoEligibleDelegatesAlert implements AlertData {
  @Inject @Transient @SchemaIgnore private transient EnvironmentService environmentService;

  @Inject @Transient @SchemaIgnore private transient CatalogService catalogService;

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

  @Override
  public String buildTitle() {
    return String.format("No delegates can execute %s tasks", getTaskTypeDisplayName());
  }

  private String getTaskTypeDisplayName() {
    List<CatalogItem> taskTypes = catalogService.getCatalogItems("TASK_TYPES");
    if (taskTypes != null) {
      Optional<CatalogItem> taskTypeCatalogItem =
          taskTypes.stream().filter(catalogItem -> catalogItem.getValue().equals(taskType.name())).findFirst();
      if (taskTypeCatalogItem.isPresent()) {
        return taskTypeCatalogItem.get().getDisplayText();
      }
    }
    return taskType.name();
  }

  public DelegateTask getTask() {
    return task;
  }

  public void setTask(DelegateTask task) {
    this.task = task;
    this.taskType = task.getTaskType().getTaskGroup();
  }

  public TaskGroup getTaskType() {
    return taskType;
  }

  public static final class NoEligibleDelegatesAlertBuilder {
    private NoEligibleDelegatesAlert noEligibleDelegatesAlert;

    private NoEligibleDelegatesAlertBuilder() {
      noEligibleDelegatesAlert = new NoEligibleDelegatesAlert();
    }

    public static NoEligibleDelegatesAlertBuilder aNoEligibleDelegatesAlert() {
      return new NoEligibleDelegatesAlertBuilder();
    }

    public NoEligibleDelegatesAlertBuilder withTask(DelegateTask task) {
      noEligibleDelegatesAlert.setTask(task);
      return this;
    }

    public NoEligibleDelegatesAlertBuilder but() {
      return aNoEligibleDelegatesAlert().withTask(noEligibleDelegatesAlert.getTask());
    }

    public NoEligibleDelegatesAlert build() {
      return noEligibleDelegatesAlert;
    }
  }
}
