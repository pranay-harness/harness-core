package io.harness.batch.processing.service.impl;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.harness.batch.processing.service.intfc.SettingValueService;
import org.springframework.beans.factory.annotation.Autowired;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;
import software.wings.settings.SettingValue;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class SettingValueServiceImpl implements SettingValueService {
  private final CloudToHarnessMappingService cloudToHarnessMappingService;

  private Cache<String, SettingValue> settingValueCache =
      Caffeine.newBuilder().expireAfterWrite(24, TimeUnit.HOURS).build();

  @Autowired
  public SettingValueServiceImpl(CloudToHarnessMappingService cloudToHarnessMappingService) {
    this.cloudToHarnessMappingService = cloudToHarnessMappingService;
  }

  @Override
  public SettingValue getSettingValueService(String settingId) {
    return settingValueCache.get(settingId, key -> getSettingAttributeFromId(key));
  }

  SettingValue getSettingAttributeFromId(String settingId) {
    Optional<SettingAttribute> settingAttributeMaybe = cloudToHarnessMappingService.getSettingAttribute(settingId);
    if (settingAttributeMaybe.isPresent()) {
      SettingAttribute settingAttribute = settingAttributeMaybe.get();
      return settingAttribute.getValue();
    }
    return null;
  }
}
