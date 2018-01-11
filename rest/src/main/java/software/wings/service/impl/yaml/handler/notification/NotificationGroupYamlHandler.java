package software.wings.service.impl.yaml.handler.notification;

import com.google.common.collect.Maps;
import com.google.inject.Inject;

import software.wings.beans.ErrorCode;
import software.wings.beans.NotificationChannelType;
import software.wings.beans.NotificationGroup;
import software.wings.beans.NotificationGroup.AddressYaml;
import software.wings.beans.NotificationGroup.NotificationGroupBuilder;
import software.wings.beans.NotificationGroup.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.service.YamlHelper;
import software.wings.service.intfc.NotificationSetupService;
import software.wings.utils.Util;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class NotificationGroupYamlHandler extends BaseYamlHandler<Yaml, NotificationGroup> {
  @Inject YamlHelper yamlHelper;
  @Inject NotificationSetupService notificationSetupService;

  private NotificationGroup toBean(ChangeContext<Yaml> changeContext) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    String accountId = changeContext.getChange().getAccountId();
    String appId = yamlHelper.getAppId(accountId, changeContext.getChange().getFilePath());

    Map<NotificationChannelType, List<String>> addressByChannelTypeMap = Maps.newHashMap();
    if (yaml.getAddresses() != null) {
      addressByChannelTypeMap = toAddressByChannelTypeMap(yaml.getAddresses());
    }
    return NotificationGroupBuilder.aNotificationGroup()
        .withAppId(appId)
        .withAccountId(accountId)
        .withAddressesByChannelType(addressByChannelTypeMap)
        .withEditable(yaml.isEditable())
        .withName(yaml.getName())
        .build();
    //        .withRoles()
  }

  @Override
  public Yaml toYaml(NotificationGroup bean, String appId) {
    List<AddressYaml> addressYamlList = toAddressYamlList(bean.getAddressesByChannelType());
    return Yaml.Builder.anYaml()
        .withAccountId(bean.getAccountId())
        .withAddresses(addressYamlList)
        .withEditable(bean.isEditable())
        .withName(bean.getName())
        .build();
  }

  @Override
  public NotificationGroup upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext);
  }

  private List<AddressYaml> toAddressYamlList(Map<NotificationChannelType, List<String>> addressesByChannelType) {
    return addressesByChannelType.entrySet().stream().map(entry -> toAddressYaml(entry)).collect(Collectors.toList());
  }

  private AddressYaml toAddressYaml(Entry<NotificationChannelType, List<String>> entry) {
    return AddressYaml.Builder.anAddressYaml()
        .withAddresses(entry.getValue())
        .withChannelType(entry.getKey().name())
        .build();
  }

  private Map<NotificationChannelType, List<String>> toAddressByChannelTypeMap(List<AddressYaml> addressYamlList) {
    return addressYamlList.stream().collect(Collectors.toMap(addressYaml
        -> Util.getEnumFromString(NotificationChannelType.class, addressYaml.getChannelType()),
        addressYaml -> addressYaml.getAddresses()));
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
  }

  @Override
  public Class getYamlClass() {
    return NotificationGroup.Yaml.class;
  }

  @Override
  public NotificationGroup get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
