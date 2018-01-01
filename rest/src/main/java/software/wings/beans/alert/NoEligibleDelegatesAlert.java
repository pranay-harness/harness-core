package software.wings.beans.alert;

import static org.apache.commons.lang.StringUtils.isNotBlank;
import static software.wings.beans.Base.GLOBAL_APP_ID;

import com.google.inject.Inject;

import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.lang.StringUtils;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.Application;
import software.wings.beans.CatalogItem;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.TaskGroup;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;
import java.util.Optional;

public class NoEligibleDelegatesAlert implements AlertData {
  @Inject @Transient @SchemaIgnore private transient EnvironmentService environmentService;

  @Inject @Transient @SchemaIgnore private transient AppService appService;

  @Inject @Transient @SchemaIgnore private transient InfrastructureMappingService infrastructureMappingService;

  @Inject @Transient @SchemaIgnore private transient CatalogService catalogService;

  private String appId;
  private String envId;
  private String infraMappingId;
  private TaskGroup taskGroup;

  @Override
  public boolean matches(AlertData alertData) {
    NoEligibleDelegatesAlert otherAlertData = (NoEligibleDelegatesAlert) alertData;

    boolean match = taskGroup == otherAlertData.getTaskGroup() && StringUtils.equals(appId, otherAlertData.getAppId())
        && StringUtils.equals(envId, otherAlertData.getEnvId())
        && StringUtils.equals(infraMappingId, otherAlertData.getInfraMappingId());

    if (match && isNotBlank(appId) && isNotBlank(envId)) {
      match = environmentService.get(appId, envId, false).getEnvironmentType()
          == environmentService.get(otherAlertData.getAppId(), otherAlertData.getEnvId(), false).getEnvironmentType();
    }

    return match;
  }

  @Override
  public String buildTitle() {
    StringBuilder title = new StringBuilder();
    title.append("No delegates can execute ").append(getTaskTypeDisplayName()).append(" tasks ");
    if (isNotBlank(appId) && !appId.equals(GLOBAL_APP_ID)) {
      Application app = appService.get(appId);
      title.append("for application ").append(app.getName()).append(" ");
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
    return title.toString();
  }

  private String getTaskTypeDisplayName() {
    List<CatalogItem> taskTypes = catalogService.getCatalogItems("TASK_TYPES");
    if (taskTypes != null) {
      Optional<CatalogItem> taskTypeCatalogItem =
          taskTypes.stream().filter(catalogItem -> catalogItem.getValue().equals(taskGroup.name())).findFirst();
      if (taskTypeCatalogItem.isPresent()) {
        return taskTypeCatalogItem.get().getDisplayText();
      }
    }
    return taskGroup.name();
  }

  public String getAppId() {
    return appId;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public String getEnvId() {
    return envId;
  }

  public void setEnvId(String envId) {
    this.envId = envId;
  }

  public String getInfraMappingId() {
    return infraMappingId;
  }

  public void setInfraMappingId(String infraMappingId) {
    this.infraMappingId = infraMappingId;
  }

  public TaskGroup getTaskGroup() {
    return taskGroup;
  }

  public void setTaskGroup(TaskGroup taskGroup) {
    this.taskGroup = taskGroup;
  }

  public static final class NoEligibleDelegatesAlertBuilder {
    private NoEligibleDelegatesAlert noEligibleDelegatesAlert;

    private NoEligibleDelegatesAlertBuilder() {
      noEligibleDelegatesAlert = new NoEligibleDelegatesAlert();
    }

    public static NoEligibleDelegatesAlertBuilder aNoEligibleDelegatesAlert() {
      return new NoEligibleDelegatesAlertBuilder();
    }

    public NoEligibleDelegatesAlertBuilder withAppId(String appId) {
      noEligibleDelegatesAlert.setAppId(appId);
      return this;
    }

    public NoEligibleDelegatesAlertBuilder withEnvId(String envId) {
      noEligibleDelegatesAlert.setEnvId(envId);
      return this;
    }

    public NoEligibleDelegatesAlertBuilder withInfraMappingId(String infraMappingId) {
      noEligibleDelegatesAlert.setInfraMappingId(infraMappingId);
      return this;
    }

    public NoEligibleDelegatesAlertBuilder withTaskGroup(TaskGroup taskGroup) {
      noEligibleDelegatesAlert.setTaskGroup(taskGroup);
      return this;
    }

    public NoEligibleDelegatesAlertBuilder but() {
      return aNoEligibleDelegatesAlert()
          .withAppId(noEligibleDelegatesAlert.getAppId())
          .withEnvId(noEligibleDelegatesAlert.getEnvId())
          .withInfraMappingId(noEligibleDelegatesAlert.getInfraMappingId())
          .withTaskGroup(noEligibleDelegatesAlert.getTaskGroup());
    }

    public NoEligibleDelegatesAlert build() {
      return noEligibleDelegatesAlert;
    }
  }
}
