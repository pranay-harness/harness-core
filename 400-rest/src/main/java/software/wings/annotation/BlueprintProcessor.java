/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.annotation;

import static io.harness.annotations.dev.HarnessModule._870_CG_ORCHESTRATION;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;

import software.wings.beans.InfrastructureMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.experimental.UtilityClass;

@UtilityClass
@TargetModule(_870_CG_ORCHESTRATION)
@OwnedBy(CDP)
public class BlueprintProcessor {
  public static void validateKeys(InfrastructureMapping infraStructureMapping, Map<String, Object> blueprints) {
    if (isEmpty(blueprints)) {
      return;
    }
    Set<String> blueprintKeys = new HashSet<>(blueprints.keySet());
    Class aClass = infraStructureMapping.getClass();
    forClass(blueprintKeys, aClass);
    while (aClass.getSuperclass() != InfrastructureMapping.class) {
      aClass = aClass.getSuperclass();
      forClass(blueprintKeys, aClass);
    }
    if (!blueprintKeys.isEmpty()) {
      throw new InvalidRequestException("Invalid blueprint keys : " + blueprintKeys.toString());
    }
  }

  private static void forClass(Set<String> blueprintKeys, Class aClass) {
    Field[] declaredFields = aClass.getDeclaredFields();
    for (Field field : declaredFields) {
      Annotation[] annotations = field.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation.annotationType() == Blueprint.class) {
          blueprintKeys.remove(field.getName());
        }
      }
    }
  }
}
