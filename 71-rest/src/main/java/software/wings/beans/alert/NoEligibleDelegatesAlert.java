package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.CatalogItem;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.TaskGroup;
import software.wings.beans.TaskType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;
import java.util.Optional;

@Data
@Builder
public class NoEligibleDelegatesAlert implements AlertData {
  @Inject @Transient @SchemaIgnore private transient EnvironmentService environmentService;
  @Inject @Transient @SchemaIgnore private transient AppService appService;
  @Inject @Transient @SchemaIgnore private transient InfrastructureMappingService infrastructureMappingService;
  @Inject @Transient @SchemaIgnore private transient CatalogService catalogService;

  private String accountId;
  private String appId;
  private String envId;
  private String infraMappingId;
  private TaskGroup taskGroup;
  private TaskType taskType;
  private List<String> tags;

  @Override
  public boolean matches(AlertData alertData) {
    NoEligibleDelegatesAlert otherAlertData = (NoEligibleDelegatesAlert) alertData;

    return StringUtils.equals(accountId, otherAlertData.getAccountId()) && taskGroup == otherAlertData.getTaskGroup()
        && taskType == otherAlertData.getTaskType() && StringUtils.equals(appId, otherAlertData.getAppId())
        && StringUtils.equals(envId, otherAlertData.getEnvId())
        && StringUtils.equals(infraMappingId, otherAlertData.getInfraMappingId())
        && ((isEmpty(tags) && isEmpty(otherAlertData.getTags()))
               || (isNotEmpty(tags) && isNotEmpty(otherAlertData.getTags())
                      && tags.containsAll(otherAlertData.getTags()) && otherAlertData.getTags().containsAll(tags)));
  }

  @Override
  public String buildTitle() {
    StringBuilder title = new StringBuilder(128);
    title.append("No delegates can execute ").append(getTaskTypeDisplayName()).append(" tasks ");
    if (isNotBlank(appId) && !appId.equals(GLOBAL_APP_ID)) {
      Application app = appService.get(appId);
      title.append("for application ").append(app.getName()).append(' ');
      if (isNotBlank(envId)) {
        Environment env = environmentService.get(app.getAppId(), envId, false);
        title.append("in ")
            .append(env.getName())
            .append(" environment (")
            .append(env.getEnvironmentType().name())
            .append(") ");
      }
      if (isNotBlank(infraMappingId)) {
        InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(app.getAppId(), infraMappingId);
        title.append("with service infrastructure ").append(infrastructureMapping.getName());
      }
    }
    if (isNotEmpty(tags)) {
      title.append(" with tags ").append(tags);
    }
    return title.toString();
  }

  private String getTaskTypeDisplayName() {
    List<CatalogItem> taskTypes = catalogService.getCatalogItems("TASK_TYPES");
    String taskTypeName = taskType != null ? " (" + taskType.name() + ")" : "";
    if (taskTypes != null) {
      Optional<CatalogItem> taskTypeCatalogItem =
          taskTypes.stream().filter(catalogItem -> catalogItem.getValue().equals(taskGroup.name())).findFirst();
      if (taskTypeCatalogItem.isPresent()) {
        return taskTypeCatalogItem.get().getDisplayText() + taskTypeName;
      }
    }
    return taskGroup.name() + taskTypeName;
  }

  @Override
  public String toString() {
    return "NoEligibleDelegatesAlert{"
        + "accountId='" + accountId + '\'' + ", appId='" + appId + '\'' + ", envId='" + envId + '\''
        + ", infraMappingId='" + infraMappingId + '\'' + ", taskGroup=" + taskGroup + ", taskType=" + taskType
        + ", tags=" + tags + '}';
  }
}
