package io.harness.pcf;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.pcf.model.ManifestType.APPLICATION_MANIFEST;
import static io.harness.pcf.model.ManifestType.APPLICATION_MANIFEST_WITH_CREATE_SERVICE;
import static io.harness.pcf.model.ManifestType.CREATE_SERVICE_MANIFEST;
import static io.harness.pcf.model.ManifestType.VARIABLE_MANIFEST;
import static io.harness.pcf.model.PcfConstants.APPLICATION_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.CREATE_SERVICE_MANIFEST_ELEMENT;
import static io.harness.pcf.model.PcfConstants.MEMORY_MANIFEST_YML_ELEMENT;
import static io.harness.pcf.model.PcfConstants.NAME_MANIFEST_YML_ELEMENT;

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
      if (isCreateServiceManifest(map)) {
        return APPLICATION_MANIFEST_WITH_CREATE_SERVICE;
      }
      return APPLICATION_MANIFEST;
    }

    if (isCreateServiceManifest(map)) {
      return CREATE_SERVICE_MANIFEST;
    }

    if (isVariableManifest(map)) {
      return VARIABLE_MANIFEST;
    }

    return null;
  }

  private boolean isVariableManifest(Map<String, Object> map) {
    Optional entryOptional = map.entrySet().stream().filter(entry -> isInvalidValue(entry.getValue())).findFirst();

    return !entryOptional.isPresent();
  }

  private boolean isInvalidValue(Object value) {
    return value instanceof Map;
  }

  private boolean isCreateServiceManifest(Map<String, Object> map) {
    return map.containsKey(CREATE_SERVICE_MANIFEST_ELEMENT);
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
