package io.harness.common;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidArgumentsException;
import lombok.experimental.UtilityClass;

import java.util.List;

@UtilityClass
public class EntityReferenceHelper {
  public static String createFQN(List<String> hierarchyList) {
    StringBuilder fqnString = new StringBuilder();
    hierarchyList.forEach(s -> {
      if (EmptyPredicate.isEmpty(s)) {
        throw new InvalidArgumentsException("Hierarchy identifier cannot be empty/null");
      }
      fqnString.append(s).append("/");
    });
    return fqnString.toString();
  }
}
