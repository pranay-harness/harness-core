package io.harness.common;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.exception.InvalidArgumentsException;

import java.util.List;
import lombok.experimental.UtilityClass;

@UtilityClass
public class EntityReferenceHelper {
  public static String createFQN(List<String> hierarchyList) {
    StringBuilder fqnString = new StringBuilder(32);
    hierarchyList.forEach(s -> {
      if (hasNone(s)) {
        throw new InvalidArgumentsException("Hierarchy identifier cannot be empty/null");
      }
      fqnString.append(s).append('/');
    });
    return fqnString.toString();
  }
}
