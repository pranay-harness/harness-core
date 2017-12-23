package software.wings.service.impl;

import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.dl.PageRequest.Builder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Base;
import software.wings.beans.CanaryOrchestrationWorkflow;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by rishi on 10/30/16.
 */
@Singleton
@ValidateOnExecution
public class NotificationSetupServiceImpl implements NotificationSetupService {
  @Inject private WingsPersistence wingsPersistence;

  @Inject private SettingsService settingsService;

  @Inject private WorkflowService workflowService;

  /**
   * Gets supported channel type details.
   *
   * @param accountId the app id
   * @return the supported channel type details
   */
  public Map<NotificationChannelType, Object> getSupportedChannelTypeDetails(String accountId) {
    Map<NotificationChannelType, Object> supportedChannelTypeDetails = new HashMap<>();
    for (NotificationChannelType notificationChannelType : NotificationChannelType.values()) {
      if (notificationChannelType.getSettingVariableTypes() != null) {
        List<SettingAttribute> settingAttributes = settingsService.getSettingAttributesByType(
            accountId, notificationChannelType.getSettingVariableTypes().name());
        if (settingAttributes != null && !settingAttributes.isEmpty()) {
          supportedChannelTypeDetails.put(notificationChannelType, new Object());
          // Put more details for the given notificationChannelType, else leave it as blank object.
        }
      }
    }
    return supportedChannelTypeDetails;
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId) {
    return listNotificationGroups(aPageRequest().addFilter("accountId", Operator.EQ, accountId).build()).getResponse();
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, Role role, String name) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter("accountId", Operator.EQ, accountId)
                                      .addFilter("roles", Operator.IN, role)
                                      .addFilter("name", Operator.EQ, name)
                                      .build())
        .getResponse();
  }

  @Override
  public PageResponse<NotificationGroup> listNotificationGroups(PageRequest<NotificationGroup> pageRequest) {
    return wingsPersistence.query(NotificationGroup.class, pageRequest);
  }

  @Override
  public NotificationGroup readNotificationGroup(String accountId, String notificationGroupId) {
    return wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public NotificationGroup readNotificationGroupByName(String accountId, String notificationGroupName) {
    PageRequest<NotificationGroup> pageRequest = PageRequest.Builder.aPageRequest()
                                                     .addFilter("accountId", Operator.EQ, accountId)
                                                     .addFilter("name", Operator.EQ, notificationGroupName)
                                                     .build();
    return wingsPersistence.get(NotificationGroup.class, pageRequest);
  }

  @Override
  public NotificationGroup createNotificationGroup(NotificationGroup notificationGroup) {
    return Validator.duplicateCheck(()
                                        -> wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup),
        "name", notificationGroup.getName());
  }

  @Override
  public NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup) {
    NotificationGroup existingGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroup.getUuid());
    if (!existingGroup.isEditable()) {
      throw new WingsException(INVALID_REQUEST, "message", "Default Notification Group can not be updated");
    }
    return wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup); // TODO:: selective update
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId) {
    NotificationGroup notificationGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
    if (!notificationGroup.isEditable()) {
      throw new WingsException(INVALID_REQUEST, "message", "Default Notification group can not be deleted");
    }

    List<Workflow> workflows =
        workflowService
            .listWorkflows(aPageRequest()
                               .withLimit(PageRequest.UNLIMITED)
                               .addFilter("workflowType", Operator.EQ, WorkflowType.ORCHESTRATION)
                               .build())
            .getResponse();
    List<String> inUse = workflows.stream()
                             .filter(workflow
                                 -> workflow.getOrchestrationWorkflow() instanceof CanaryOrchestrationWorkflow
                                     && ((CanaryOrchestrationWorkflow) workflow.getOrchestrationWorkflow())
                                            .getNotificationRules()
                                            .stream()
                                            .anyMatch(notificationRule
                                                -> notificationRule.getNotificationGroups().stream().anyMatch(
                                                    ng -> ng.getUuid().equals(notificationGroupId))))
                             .map(Workflow::getName)
                             .collect(Collectors.toList());
    if (!inUse.isEmpty()) {
      throw new WingsException(INVALID_REQUEST, "message",
          String.format("'%s' is in use by %s workflow%s: '%s'", notificationGroup.getName(), inUse.size(),
              inUse.size() == 1 ? "" : "s", Joiner.on("', '").join(inUse)));
    }
    return wingsPersistence.delete(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, String name) {
    return listNotificationGroups(
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("name", Operator.EQ, name).build())
        .getResponse();
  }
}
