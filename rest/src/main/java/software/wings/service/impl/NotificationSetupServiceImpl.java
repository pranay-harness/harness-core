package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.ErrorCode.INVALID_REQUEST;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

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
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
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
  @Inject private YamlChangeSetHelper yamlChangeSetHelper;

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
        if (isNotEmpty(settingAttributes)) {
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
    PageRequest<NotificationGroup> pageRequest = aPageRequest()
                                                     .addFilter("accountId", Operator.EQ, accountId)
                                                     .addFilter("name", Operator.EQ, notificationGroupName)
                                                     .build();
    return wingsPersistence.get(NotificationGroup.class, pageRequest);
  }

  @Override
  public NotificationGroup createNotificationGroup(NotificationGroup notificationGroup) {
    checkIfChangeInDefaultNotificationGroup(notificationGroup);
    NotificationGroup savedNotificationGroup =
        Validator.duplicateCheck(()
                                     -> wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup),
            "name", notificationGroup.getName());

    yamlChangeSetHelper.notificationGroupYamlChangeAsync(savedNotificationGroup, ChangeType.ADD);
    return savedNotificationGroup;
  }

  @Override
  public NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup) {
    checkIfChangeInDefaultNotificationGroup(notificationGroup);
    NotificationGroup existingGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroup.getUuid());
    if (!existingGroup.isEditable()) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "Default Notification Group can not be updated");
    }
    NotificationGroup updatedGroup =
        wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup); // TODO:: selective update

    yamlChangeSetHelper.updateYamlChangeAsync(updatedGroup, existingGroup, updatedGroup.getAccountId());
    return updatedGroup;
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId) {
    NotificationGroup notificationGroup =
        wingsPersistence.get(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
    if (!notificationGroup.isEditable()) {
      throw new WingsException(INVALID_REQUEST).addParam("message", "Default Notification group can not be deleted");
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
      throw new WingsException(INVALID_REQUEST)
          .addParam("message",
              String.format("'%s' is in use by %s workflow%s: '%s'", notificationGroup.getName(), inUse.size(),
                  inUse.size() == 1 ? "" : "s", Joiner.on("', '").join(inUse)));
    }

    yamlChangeSetHelper.notificationGroupYamlChangeSet(notificationGroup, ChangeType.DELETE);
    return wingsPersistence.delete(NotificationGroup.class, Base.GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, String name) {
    return listNotificationGroups(
        aPageRequest().addFilter("accountId", Operator.EQ, accountId).addFilter("name", Operator.EQ, name).build())
        .getResponse();
  }

  @Override
  public List<NotificationGroup> listDefaultNotificationGroup(String accountId) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter("accountId", Operator.EQ, accountId)
                                      .addFilter("defaultNotificationGroupForAccount", Operator.EQ, true)
                                      .build())
        .getResponse();
  }

  /**
   * There can be only 1 default notification group per account.
   * So while a notification group is being created or updated, where isDefault = true,
   * this method will check if there exists any notification group that is set as default,
   * and will set its default=false.
   * @param notificationGroup
   */
  private void checkIfChangeInDefaultNotificationGroup(NotificationGroup notificationGroup) {
    List<NotificationGroup> notificationGroups = null;
    if (notificationGroup.isDefaultNotificationGroupForAccount()
        && isNotEmpty(notificationGroups = listDefaultNotificationGroup(notificationGroup.getAccountId()))) {
      NotificationGroup previousDefaultNotificationGroup = notificationGroups.get(0);
      // make sure, previous and current one being saved/updated is not the same
      if (!previousDefaultNotificationGroup.getName().equals(notificationGroup.getName())) {
        previousDefaultNotificationGroup.setDefaultNotificationGroupForAccount(false);
        // updateNotificationGroup() will call checkIfChangeInDefaultNotificationGroup() again, but this time,
        // as previousDefaultNotificationGroup.IsDefault = false, it will not even enter top "if" condition and exit
        // method immediately. Reason we need to go through entire updateNotificationGroup() flow is, it also calls
        // yamlUpdate.
        updateNotificationGroup(previousDefaultNotificationGroup);
      }
    }
  }
}
