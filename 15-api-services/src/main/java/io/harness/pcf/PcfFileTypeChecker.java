package io.harness.pcf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pcf.model.ManifestType.APPLICATION_MANIFEST;
import static io.harness.pcf.model.ManifestType.AUTOSCALAR_MANIFEST;
import static io.harness.pcf.model.ManifestType.VARIABLE_MANIFEST;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.MEMORY_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.PCF_AUTOSCALAR_MANIFEST_INSTANCE_LIMITS_ELE;
import static io.harness.pcf.model.PcfConstants.PCF_AUTOSCALAR_MANIFEST_RULES_ELE;

import com.google.inject.Singleton;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import io.harness.pcf.model.ManifestType;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

@Singleton
@Slf4j
public class PcfFileTypeChecker {
  private static final Yaml yaml;

  static {
    yaml = new Yaml();
  }

  public ManifestType getManifestType(String content) {
    Map<String, Object> map = null;
    try {
      map = (Map<String, Object>) yaml.load(content);
    } catch (Exception e) {
      return null;
    }

    if (isApplicationManifest(map)) {
      return APPLICATION_MANIFEST;
    }

    if (isVariableManifest(map)) {
      return VARIABLE_MANIFEST;
    }

    if (isAutoscalarManifest(map)) {
      return AUTOSCALAR_MANIFEST;
    }

    return null;
  }

  private boolean isAutoscalarManifest(Map<String, Object> map) {
    return map.containsKey(PCF_AUTOSCALAR_MANIFEST_INSTANCE_LIMITS_ELE)
        && map.containsKey(PCF_AUTOSCALAR_MANIFEST_RULES_ELE);
  }

  private boolean isVariableManifest(Map<String, Object> map) {
    Optional entryOptional = map.entrySet().stream().filter(entry -> isInvalidValue(entry.getValue())).findFirst();

    return !entryOptional.isPresent();
  }

  private boolean isInvalidValue(Object value) {
    return value instanceof Map;
  }

  private boolean isApplicationManifest(Map<String, Object> map) {
    if (map.containsKey(APPLICATION_YML_ELEMENT)) {
      List<Map> applicationMaps = (List<Map>) map.get(APPLICATION_YML_ELEMENT);
      if (isEmpty(applicationMaps)) {
        return false;
      }

      Map application = applicationMaps.get(0);
      Map<String, Object> treeMap = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
      treeMap.putAll(application);
      if (treeMap.containsKey(NAME_MANIFEST_YML_ELEMENT) && treeMap.containsKey(MEMORY_MANIFEST_YML_ELEMENT)) {
        return true;
      }
    }

    return false;
  }
}
