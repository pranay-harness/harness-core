package io.harness.reflection;

import static io.harness.exception.WingsException.USER_SRE;
import static java.lang.String.format;

import io.harness.exception.InvalidArgumentsException;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
              } else if (object instanceof Map) {
                Map objectMap = (Map) object;
                objectMap.replaceAll((k, v) -> functor.update((String) v));
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

  /**
   * Update string values inside object recursively. The new value is obtained using the functor. If the skipPredicate
   * returns true for a field, it is skipped.
   *
   * @param o             the object to update
   * @param skipPredicate the predicate which checks if a field should be skipped
   * @param functor       the functor which provides the new value
   * @return the new object with updated strings (this can be done in-place or a new object can be returned)
   */
  public static Object updateStrings(Object o, Predicate<Field> skipPredicate, Functor functor) {
    Object updatedObj = updateStringInternal(o, skipPredicate, functor, new HashSet<>());
    return updatedObj == null ? o : updatedObj;
  }

  private static Object updateStringInternal(
      Object obj, Predicate<Field> skipPredicate, Functor functor, Set<Integer> cache) {
    if (obj == null) {
      return null;
    }

    Class<?> c = obj.getClass();
    if (ClassUtils.isPrimitiveOrWrapper(c)) {
      return null;
    }

    if (obj instanceof String) {
      String oldVal = (String) obj;
      String newVal = functor.update(oldVal);
      return oldVal.equals(newVal) ? null : newVal;
    }

    // In case of array, update in-place and return null.
    if (c.isArray()) {
      if (c.getComponentType().isPrimitive()) {
        return false;
      }

      int length = Array.getLength(obj);
      for (int i = 0; i < length; i++) {
        Object arrObj = Array.get(obj, i);
        Object newArrObj = updateStringInternal(arrObj, skipPredicate, functor, cache);
        if (newArrObj != null) {
          Array.set(obj, i, newArrObj);
        }
      }

      return null;
    }

    // In case of object, iterate over fields and update them in a similar manner.
    boolean updated = updateStringFields(obj, skipPredicate, functor, cache);
    if (!updated) {
      return null;
    }

    return obj;
  }

  private static boolean updateStringFields(
      Object obj, Predicate<Field> skipPredicate, Functor functor, Set<Integer> cache) {
    if (obj == null) {
      return false;
    }

    int hashCode = System.identityHashCode(obj);
    if (cache.contains(hashCode)) {
      return false;
    } else {
      cache.add(hashCode);
    }

    Class<?> c = obj.getClass();
    boolean updated = false;
    while (c.getSuperclass() != null) {
      for (Field f : c.getDeclaredFields()) {
        // Ignore field if skipPredicate returns true or if the field is static.
        if (skipPredicate.test(f) || Modifier.isStatic(f.getModifiers())) {
          continue;
        }

        boolean isAccessible = f.isAccessible();
        f.setAccessible(true);
        try {
          if (updateStringFieldsInternal(obj, f, skipPredicate, functor, cache)) {
            updated = true;
          }
          f.setAccessible(isAccessible);
        } catch (IllegalAccessException ignored) {
          logger.error("Field [{}] is not accessible", f.getName());
        }
      }
      c = c.getSuperclass();
    }

    return updated;
  }

  private static boolean updateStringFieldsInternal(Object o, Field f, Predicate<Field> skipPredicate, Functor functor,
      Set<Integer> cache) throws IllegalAccessException {
    if (f == null) {
      return false;
    }

    Object obj = f.get(o);
    Object updatedObj = updateStringInternal(obj, skipPredicate, functor, cache);
    if (updatedObj != null) {
      f.set(o, updatedObj);
      return true;
    }

    return false;
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

  public static Object getFieldValue(@NonNull Object obj, @NonNull Field field) {
    try {
      return FieldUtils.readField(field, obj, true);
    } catch (IllegalAccessException e) {
      logger.error("Unable to access field {} in object {}", field.getName(), obj.getClass(), e);
    }
    return null;
  }
}
