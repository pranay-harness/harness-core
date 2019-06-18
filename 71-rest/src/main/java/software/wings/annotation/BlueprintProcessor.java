package software.wings.annotation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.exception.InvalidRequestException;
import software.wings.beans.InfrastructureMapping;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class BlueprintProcessor {
  public static void validateKeys(InfrastructureMapping infraStructureMapping, Map<String, Object> blueprints) {
    if (isEmpty(blueprints)) {
      return;
    }
    Set<String> blueprintKeys = new HashSet<>(blueprints.keySet());
    Class<? extends InfrastructureMapping> aClass = infraStructureMapping.getClass();
    Field[] declaredFields = aClass.getDeclaredFields();
    for (Field field : declaredFields) {
      Annotation[] annotations = field.getAnnotations();
      for (Annotation annotation : annotations) {
        if (annotation.annotationType() == Blueprint.class) {
          blueprintKeys.remove(field.getName());
        }
      }
    }
    if (!blueprintKeys.isEmpty()) {
      throw new InvalidRequestException("Invalid blueprint keys : " + blueprintKeys.toString());
    }
  }
}