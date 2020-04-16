package io.harness.reflection;

import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;

import io.harness.exception.InvalidArgumentsException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.annotation.Nonnull;

@Slf4j
public class ReflectionUtils {
  public static Field getFieldByName(Class<?> clazz, String fieldName) {
    while (clazz.getSuperclass() != null) {
      try {
        return clazz.getDeclaredField(fieldName);
      } catch (NoSuchFieldException e) {
        clazz = clazz.getSuperclass();
        continue;
      }
    }
    return null;
  }

  public static List<Field> getAllDeclaredAndInheritedFields(Class<?> clazz) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      Collections.addAll(declaredFields, clazz.getDeclaredFields());
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }

  public static List<Field> getDeclaredAndInheritedFields(Class<?> clazz, Predicate<Field> predicate) {
    List<Field> declaredFields = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      for (Field f : clazz.getDeclaredFields()) {
        if (predicate.test(f)) {
          declaredFields.add(f);
        }
      }
      clazz = clazz.getSuperclass();
    }
    return declaredFields;
  }

  public interface Functor { String update(String o); }

  public static void updateFieldValues(Object o, Predicate<Field> predicate, Functor functor) {
    Class<?> c = o.getClass();
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        if (predicate.test(f)) {
          boolean isAccessible = f.isAccessible();
          f.setAccessible(true);
          try {
            Object object = f.get(o);
            if (object != null) {
              if (object instanceof List) {
                List objectList = (List) object;
                for (int i = 0; i < objectList.size(); i++) {
                  objectList.set(i, functor.update((String) objectList.get(i)));
                }
              } else {
                String value = functor.update((String) object);
                f.set(o, value);
              }
            }
            f.setAccessible(isAccessible);
          } catch (IllegalAccessException e) {
            logger.error("Field [{}] is not accessible ", f.getName());
          }
        }
      }
      c = c.getSuperclass();
    }
  }

  public static String getAccessorFieldName(String methodName) {
    if (methodName.startsWith("get") && methodName.length() > 3) {
      return Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
    } else if (methodName.startsWith("is") && methodName.length() > 2) {
      return Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
    }

    throw new InvalidArgumentsException("Invalid accessor method name", USER_SRE);
  }

  public static List<Method> getAccessorMethods(Class<?> clazz) {
    List<Method> methods = new ArrayList<>();
    while (clazz.getSuperclass() != null) {
      for (Method m : clazz.getDeclaredMethods()) {
        if (!Modifier.isPublic(m.getModifiers())) {
          continue;
        }
        boolean shouldBeBoolean = false;
        String fieldName;

        final String methodName = m.getName();
        if (methodName.startsWith("get") && methodName.length() > 3) {
          fieldName = Character.toLowerCase(methodName.charAt(3)) + methodName.substring(4);
        } else if (methodName.startsWith("is") && methodName.length() > 2) {
          fieldName = Character.toLowerCase(methodName.charAt(2)) + methodName.substring(3);
          shouldBeBoolean = true;
        } else {
          continue;
        }

        try {
          final Field declaredField = clazz.getDeclaredField(fieldName);
          if (shouldBeBoolean
              != (declaredField.getType() == boolean.class || declaredField.getType() == Boolean.class)) {
            continue;
          }
        } catch (NoSuchFieldException e) {
          continue;
        }

        methods.add(m);
      }
      clazz = clazz.getSuperclass();
    }
    return methods;
  }

  public static Map<String, Object> getFieldValues(@Nonnull Object obj, @Nonnull Set<String> fieldNames) {
    Map<String, Object> fieldNameValueMap = new HashMap<>();
    for (String fieldName : fieldNames) {
      try {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        Object value = field.get(obj);
        fieldNameValueMap.put(fieldName, value);
      } catch (NoSuchFieldException ignored) {
        logger.error(format("Field \"%s\" not available in object \"%s\"", fieldName, obj.toString()));
      } catch (IllegalAccessException e) {
        logger.error(format("Unable to access field \"%s\"", fieldName));
      }
    }
    return fieldNameValueMap;
  }

  public static Object getFieldValue(@NonNull Object obj, @NonNull String fieldName) {
    return getFieldValues(obj, Collections.singleton(fieldName)).get(fieldName);
  }
}
