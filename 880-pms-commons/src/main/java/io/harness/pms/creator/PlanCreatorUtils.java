package io.harness.pms.creator;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.YamlField;

import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PlanCreatorUtils {
  public final String ANY_TYPE = "__any__";

  public boolean supportsField(Map<String, Set<String>> supportedTypes, YamlField field) {
    if (EmptyPredicate.isEmpty(supportedTypes)) {
      return false;
    }

    String fieldName = field.getName();
    Set<String> types = supportedTypes.get(fieldName);
    if (EmptyPredicate.isEmpty(types)) {
      return false;
    }

    String fieldType = field.getNode().getType();
    if (EmptyPredicate.isEmpty(fieldType)) {
      fieldType = ANY_TYPE;
    }
    return types.contains(fieldType);
  }
}
