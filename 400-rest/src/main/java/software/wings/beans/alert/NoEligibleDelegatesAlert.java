/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.alert;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.Application.GLOBAL_APP_ID;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.alert.AlertData;
import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.TaskGroup;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;

import software.wings.beans.Application;
import software.wings.beans.CatalogItem;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.TaskType;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;

import com.github.reinert.jjschema.SchemaIgnore;
import com.google.inject.Inject;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.annotations.Transient;

@Data
@Builder
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
@BreakDependencyOn("software.wings.beans.Application")
@BreakDependencyOn("software.wings.beans.CatalogItem")
@BreakDependencyOn("software.wings.beans.Environment")
@BreakDependencyOn("software.wings.beans.InfrastructureMapping")
@BreakDependencyOn("software.wings.service.intfc.AppService")
@BreakDependencyOn("software.wings.service.intfc.CatalogService")
@BreakDependencyOn("software.wings.service.intfc.EnvironmentService")
@BreakDependencyOn("software.wings.service.intfc.InfrastructureMappingService")
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
  private List<ExecutionCapability> executionCapabilities;

  @Override
  public boolean matches(AlertData alertData) {
    NoEligibleDelegatesAlert otherAlertData = (NoEligibleDelegatesAlert) alertData;

    return StringUtils.equals(accountId, otherAlertData.getAccountId()) && taskGroup == otherAlertData.getTaskGroup()
        && taskType == otherAlertData.getTaskType() && StringUtils.equals(appId, otherAlertData.getAppId())
        && StringUtils.equals(envId, otherAlertData.getEnvId())
        && StringUtils.equals(infraMappingId, otherAlertData.getInfraMappingId())
        && ((isEmpty(executionCapabilities) && isEmpty(otherAlertData.getExecutionCapabilities()))
            || (isNotEmpty(executionCapabilities) && isNotEmpty(otherAlertData.getExecutionCapabilities())
                && executionCapabilities.containsAll(otherAlertData.getExecutionCapabilities())
                && otherAlertData.getExecutionCapabilities().containsAll(executionCapabilities)));
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
        title.append("with service infrastructure ").append(infrastructureMapping.getDisplayName());
      }
    }
    if (isNotEmpty(executionCapabilities)) {
      List<SelectorCapability> selectorCapabilities = executionCapabilities.stream()
                                                          .filter(c -> c instanceof SelectorCapability)
                                                          .map(c -> (SelectorCapability) c)
                                                          .collect(toList());

      for (SelectorCapability selectorCapability : selectorCapabilities) {
        Set<String> selectors = selectorCapability.getSelectors();
        String selectorOrigin = selectorCapability.getSelectorOrigin();
        title.append(" with selectors: ").append(selectors).append(", with origin: ").append(selectorOrigin);
      }
      return title.toString();
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
        + ", selectors=" + executionCapabilities + '}';
  }
}
