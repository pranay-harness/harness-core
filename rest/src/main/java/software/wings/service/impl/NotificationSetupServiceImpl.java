package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.lang.String.format;
import static org.atteo.evo.inflector.English.plural;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.base.Joiner;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Workflow;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.dl.HIterator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.service.impl.yaml.YamlChangeSetHelper;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    return wingsPersistence.get(NotificationGroup.class, GLOBAL_APP_ID, notificationGroupId);
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
        wingsPersistence.get(NotificationGroup.class, GLOBAL_APP_ID, notificationGroup.getUuid());
    if (!existingGroup.isEditable()) {
      throw new InvalidRequestException("Default Notification Group can not be updated");
    }
    NotificationGroup updatedGroup =
        wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup); // TODO:: selective update

    yamlChangeSetHelper.updateYamlChangeAsync(updatedGroup, existingGroup, updatedGroup.getAccountId());
    return updatedGroup;
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId) {
    NotificationGroup notificationGroup = wingsPersistence.get(NotificationGroup.class, notificationGroupId);
    if (!notificationGroup.isEditable()) {
      throw new InvalidRequestException("Default Notification group can not be deleted");
    }

    List<String> inUse = new ArrayList<>();

    wingsPersistence.createQuery(Application.class)
        .filter(ACCOUNT_ID_KEY, accountId)
        .asKeyList()
        .stream()
        .map(key -> key.getId().toString())
        .forEach(appId -> {
          try (HIterator<Workflow> workflows =
                   new HIterator<>(wingsPersistence.createQuery(Workflow.class).filter(APP_ID_KEY, appId).fetch())) {
            while (workflows.hasNext()) {
              Workflow workflow = workflows.next();
              if (workflow.getOrchestrationWorkflow() != null
                  && workflow.getOrchestrationWorkflow().getNotificationRules().stream().anyMatch(notificationRule
                         -> notificationRule.getNotificationGroups().stream().anyMatch(
                             ng -> ng.getUuid().equals(notificationGroupId)))) {
                inUse.add(workflow.getName());
              }
            }
          }
        });
    if (isNotEmpty(inUse)) {
      throw new InvalidRequestException(format("'%s' is in use by %d workflow%s: '%s'", notificationGroup.getName(),
          inUse.size(), plural("workflow", inUse.size()), Joiner.on("', '").join(inUse)));
    }

    yamlChangeSetHelper.notificationGroupYamlChangeSet(notificationGroup, ChangeType.DELETE);
    return wingsPersistence.delete(NotificationGroup.class, notificationGroupId);
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
