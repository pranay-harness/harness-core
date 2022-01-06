/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.notification;

import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;

import static software.wings.beans.CGConstants.GLOBAL_APP_ID;
import static software.wings.beans.ObjectType.NOTIFICATION_GROUP;

import static java.util.stream.Collectors.toList;

import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationGroup.NotificationGroupBuilder;
import software.wings.beans.NotificationGroup.Yaml;
import software.wings.beans.NotificationGroupAddressYaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.utils.Utils;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class NotificationGroupYamlHandler extends BaseYamlHandler<Yaml, NotificationGroup> {
  @Inject YamlHelper yamlHelper;
  @Inject NotificationSetupService notificationSetupService;

  private NotificationGroup toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();

    Map<NotificationChannelType, List<String>> addressByChannelTypeMap = new HashMap<>();
    if (yaml.getAddresses() != null) {
      addressByChannelTypeMap = toAddressByChannelTypeMap(yaml.getAddresses());
    }
    String notificationGroupName = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    return NotificationGroupBuilder.aNotificationGroup()
        .withAppId(GLOBAL_APP_ID)
        .withAccountId(accountId)
        .withAddressesByChannelType(addressByChannelTypeMap)
        .withEditable(true)
        .withName(notificationGroupName)
        .withDefaultNotificationGroupForAccount(parseIsDefaultFromYaml(yaml))
        .build();
    //        .withRoles()
  }

  private boolean parseIsDefaultFromYaml(Yaml yaml) {
    return Boolean.parseBoolean(yaml.getDefaultNotificationGroupForAccount());
  }

  @Override
  public Yaml toYaml(NotificationGroup bean, String appId) {
    bean = notificationSetupService.readNotificationGroup(bean.getAccountId(), bean.getUuid());
    List<NotificationGroupAddressYaml> addressYamlList = toAddressYamlList(bean.getAddressesByChannelType());
    return Yaml.builder()
        .harnessApiVersion(getHarnessApiVersion())
        .addresses(addressYamlList)
        .type(NOTIFICATION_GROUP)
        .defaultNotificationGroupForAccount(String.valueOf(bean.isDefaultNotificationGroupForAccount()))
        .build();
  }

  @Override
  public NotificationGroup upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    String accountId = changeContext.getChange().getAccountId();
    NotificationGroup previous = get(accountId, changeContext.getChange().getFilePath());

    NotificationGroup notificationGroup = toBean(changeContext);
    notificationGroup.setSyncFromGit(changeContext.getChange().isSyncFromGit());

    if (previous != null) {
      notificationGroup.setUuid(previous.getUuid());
      return notificationSetupService.updateNotificationGroup(notificationGroup);
    } else {
      return notificationSetupService.createNotificationGroup(notificationGroup);
    }
  }

  private List<NotificationGroupAddressYaml> toAddressYamlList(
      Map<NotificationChannelType, List<String>> addressesByChannelType) {
    return addressesByChannelType.entrySet().stream().map(this::toAddressYaml).collect(toList());
  }

  private NotificationGroupAddressYaml toAddressYaml(Entry<NotificationChannelType, List<String>> entry) {
    return NotificationGroupAddressYaml.builder()
        .addresses(entry.getValue())
        .channelType(entry.getKey().name())
        .build();
  }

  private Map<NotificationChannelType, List<String>> toAddressByChannelTypeMap(
      List<NotificationGroupAddressYaml> addressYamlList) {
    return addressYamlList.stream().collect(Collectors.toMap(addressYaml
        -> Utils.getEnumFromString(NotificationChannelType.class, addressYaml.getChannelType()),
        addressYaml -> {
          List<String> addressList = addressYaml.getAddresses();
          return addressList == null ? Lists.newArrayList() : addressList;
        }));
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public NotificationGroup get(String accountId, String yamlFilePath) {
    String notificationGroupName = yamlHelper.getNameFromYamlFilePath(yamlFilePath);
    return notificationSetupService.readNotificationGroupByName(accountId, notificationGroupName);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    String accountId = changeContext.getChange().getAccountId();
    String notificationGroupName = yamlHelper.getNameFromYamlFilePath(changeContext.getChange().getFilePath());
    NotificationGroup notificationGroup =
        notificationSetupService.readNotificationGroupByName(accountId, notificationGroupName);
    notNullCheck("No Notification Group exists with the given name: " + notificationGroupName, notificationGroup, USER);
    notificationSetupService.deleteNotificationGroups(
        accountId, notificationGroup.getUuid(), changeContext.getChange().isSyncFromGit());
  }
}
