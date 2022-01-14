/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.atteo.evo.inflector.English.plural;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import io.harness.validation.PersistenceValidator;

import software.wings.beans.Application;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.Event;
import software.wings.beans.Event.Type;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.Role;
import software.wings.beans.SettingAttribute;
import software.wings.beans.User;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.WorkflowService;
import software.wings.service.intfc.yaml.YamlPushService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
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
  @Inject private YamlPushService yamlPushService;
  @Inject private UserService userService;

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
    return listNotificationGroups(aPageRequest()
                                      .addFilter(NotificationGroup.ACCOUNT_ID_KEY2, Operator.EQ, accountId)
                                      .addOrder(NotificationGroup.NAME_KEY, SortOrder.OrderType.ASC)
                                      .build())
        .getResponse();
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, Role role, String name) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter(NotificationGroup.ACCOUNT_ID_KEY2, Operator.EQ, accountId)
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
    return wingsPersistence.getWithAppId(NotificationGroup.class, GLOBAL_APP_ID, notificationGroupId);
  }

  @Override
  public List<NotificationGroup> readNotificationGroups(String accountId, List<String> notificationGroupIds) {
    return wingsPersistence.createQuery(NotificationGroup.class)
        .filter(NotificationGroup.ACCOUNT_ID_KEY2, accountId)
        .field(NotificationGroup.ID_KEY2)
        .in(notificationGroupIds)
        .asList();
  }

  @Override
  public NotificationGroup readNotificationGroupByName(String accountId, String notificationGroupName) {
    return wingsPersistence.createQuery(NotificationGroup.class)
        .filter(NotificationGroup.ACCOUNT_ID_KEY2, accountId)
        .filter(NotificationGroup.NAME_KEY, notificationGroupName)
        .get();
  }

  @Override
  public NotificationGroup createNotificationGroup(NotificationGroup notificationGroup) {
    checkIfChangeInDefaultNotificationGroup(notificationGroup);
    NotificationGroup savedNotificationGroup = PersistenceValidator.duplicateCheck(
        ()
            -> wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup),
        "name", notificationGroup.getName());

    yamlPushService.pushYamlChangeSet(notificationGroup.getAccountId(), null, savedNotificationGroup, Event.Type.CREATE,
        notificationGroup.isSyncFromGit(), false);
    return savedNotificationGroup;
  }

  @Override
  public NotificationGroup updateNotificationGroup(NotificationGroup notificationGroup) {
    checkIfChangeInDefaultNotificationGroup(notificationGroup);
    NotificationGroup existingGroup =
        wingsPersistence.getWithAppId(NotificationGroup.class, GLOBAL_APP_ID, notificationGroup.getUuid());
    if (!existingGroup.isEditable()) {
      throw new InvalidRequestException("Default Notification Group can not be updated");
    }
    NotificationGroup updatedGroup =
        wingsPersistence.saveAndGet(NotificationGroup.class, notificationGroup); // TODO:: selective update

    boolean isRename = !updatedGroup.getName().equals(existingGroup.getName());
    yamlPushService.pushYamlChangeSet(updatedGroup.getAccountId(), existingGroup, updatedGroup, Type.UPDATE,
        notificationGroup.isSyncFromGit(), isRename);

    return updatedGroup;
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId) {
    return deleteNotificationGroups(accountId, notificationGroupId, false);
  }

  @Override
  public boolean deleteNotificationGroups(String accountId, String notificationGroupId, boolean syncFromGit) {
    NotificationGroup notificationGroup = wingsPersistence.get(NotificationGroup.class, notificationGroupId);
    if (!notificationGroup.isEditable()) {
      throw new InvalidRequestException("Default Notification group can not be deleted");
    }

    List<String> inUse = new ArrayList<>();

    wingsPersistence.createQuery(Application.class)
        .filter(Application.ACCOUNT_ID_KEY2, accountId)
        .asKeyList()
        .stream()
        .map(key -> key.getId().toString())
        .forEach(appId -> {
          try (HIterator<Workflow> workflows = new HIterator<>(
                   wingsPersistence.createQuery(Workflow.class).filter(WorkflowKeys.appId, appId).fetch())) {
            for (Workflow workflow : workflows) {
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
          inUse.size(), plural("workflow", inUse.size()), join("', '", inUse)));
    }

    yamlPushService.pushYamlChangeSet(accountId, notificationGroup, null, Type.DELETE, syncFromGit, false);

    return wingsPersistence.delete(NotificationGroup.class, notificationGroupId);
  }

  @Override
  public List<NotificationGroup> listNotificationGroups(String accountId, String name) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter(NotificationGroup.ACCOUNT_ID_KEY2, Operator.EQ, accountId)
                                      .addFilter("name", Operator.EQ, name)
                                      .build())
        .getResponse();
  }

  @Override
  public List<NotificationGroup> listDefaultNotificationGroup(String accountId) {
    return listNotificationGroups(aPageRequest()
                                      .addFilter(NotificationGroup.ACCOUNT_ID_KEY2, Operator.EQ, accountId)
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

  @Override
  public List<String> getUserEmailAddressFromNotificationGroups(
      String accountId, List<NotificationGroup> notificationGroups) {
    if (isEmpty(notificationGroups)) {
      return asList();
    }

    notificationGroups = notificationGroups.stream()
                             .map(notificationGroup -> readNotificationGroup(accountId, notificationGroup.getUuid()))
                             .filter(Objects::nonNull)
                             .filter(notificationGroup -> notificationGroup.getAddressesByChannelType() != null)
                             .collect(toList());

    List<String> emailAddresses = new ArrayList<>();
    for (NotificationGroup notificationGroup : notificationGroups) {
      if (notificationGroup.getRoles() != null) {
        notificationGroup.getRoles().forEach(role -> {
          try (HIterator<User> iterator =
                   new HIterator<>(wingsPersistence.createQuery(User.class)
                                       .filter(ApplicationKeys.appId, notificationGroup.getAppId())
                                       .field(User.ROLES_KEY)
                                       .in(asList(role))
                                       .fetch())) {
            for (User user : iterator) {
              if (user.isEmailVerified()) {
                emailAddresses.add(user.getEmail());
              }
            }
          }
        });
      }

      for (Entry<NotificationChannelType, List<String>> entry :
          notificationGroup.getAddressesByChannelType().entrySet()) {
        if (entry.getKey() == NotificationChannelType.EMAIL && isNotEmpty(entry.getValue())) {
          for (String emailAddress : entry.getValue()) {
            if (isNotBlank(emailAddress)) {
              emailAddresses.add(emailAddress);
            }
          }
        }
      }
    }

    return emailAddresses;
  }

  @Override
  public void deleteByAccountId(String accountId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(NotificationGroup.class).filter(NotificationGroup.ACCOUNT_ID_KEY2, accountId));
  }
}
