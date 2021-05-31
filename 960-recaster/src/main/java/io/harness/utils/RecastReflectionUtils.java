package io.harness.utils;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.core.AliasRegistry;
import io.harness.core.Recaster;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class RecastReflectionUtils {
  public static Field[] getDeclaredAndInheritedFields(final Class<?> type, final boolean returnFinalFields) {
    final List<Field> allFields = new ArrayList<>(getValidFields(type.getDeclaredFields(), returnFinalFields));
    Class<?> parent = type.getSuperclass();
    while ((parent != null) && (parent != Object.class)) {
      allFields.addAll(getValidFields(parent.getDeclaredFields(), returnFinalFields));
      parent = parent.getSuperclass();
    }
    return allFields.toArray(new Field[allFields.size()]);
  }

  public static List<Field> getValidFields(final Field[] fields, final boolean returnFinalFields) {
    final List<Field> validFields = new ArrayList<>();
    // we ignore static and final fields
    for (final Field field : fields) {
      if (!Modifier.isStatic(field.getModifiers()) && (returnFinalFields || !Modifier.isFinal(field.getModifiers()))) {
        validFields.add(field);
      }
    }
    return validFields;
  }

  public static <T> Class<?> getTypeArgument(
      final Class<? extends T> clazz, final TypeVariable<? extends GenericDeclaration> tv) {
    final Map<Type, Type> resolvedTypes = new HashMap<>();
    Type type = clazz;
    // start walking up the inheritance hierarchy until we hit the end
    while (type != null && !Object.class.equals(getClass(type))) {
      if (type instanceof Class) {
        // there is no useful information for us in raw types, so just
        // keep going.
        type = ((Class<?>) type).getGenericSuperclass();
      } else {
        final ParameterizedType parameterizedType = (ParameterizedType) type;
        final Class<?> rawType = (Class<?>) parameterizedType.getRawType();

        final Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        final TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        for (int i = 0; i < actualTypeArguments.length; i++) {
          if (typeParameters[i].equals(tv)) {
            final Class<?> cls = getClass(actualTypeArguments[i]);
            if (cls != null) {
              return cls;
            }
            // We don't know that the type we want is the one in the map, if this argument has been
            // passed through multiple levels of the hierarchy.  Walk back until we run out.
            Type typeToTest = resolvedTypes.get(actualTypeArguments[i]);
            while (typeToTest != null) {
              final Class<?> classToTest = getClass(typeToTest);
              if (classToTest != null) {
                return classToTest;
              }
              typeToTest = resolvedTypes.get(typeToTest);
            }
          }
          resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
        }

        if (!rawType.equals(Object.class)) {
          type = rawType.getGenericSuperclass();
        }
      }
    }

    return null;
  }

  public static Class<?> getClass(final Type type) {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return getClass(((ParameterizedType) type).getRawType());
    } else if (type instanceof GenericArrayType) {
      final Type componentType = ((GenericArrayType) type).getGenericComponentType();
      final Class<?> componentClass = getClass(componentType);
      if (componentClass != null) {
        return Array.newInstance(componentClass, 0).getClass();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> Class<T> getClass(final Document document) {
    // see if there is a className value
    Class<?> c = null;
    if (document.containsKey(Recaster.RECAST_CLASS_KEY)) {
      final String documentIdentifier = (String) getDocumentIdentifier(document);

      // lets check alias registry first
      AliasRegistry aliasRegistry = AliasRegistry.getInstance();
      Class<?> aliasClazz = aliasRegistry.obtain(documentIdentifier);
      if (aliasClazz != null) {
        return (Class<T>) aliasClazz;
      }

      try {
        c = Class.forName(documentIdentifier, true, Thread.currentThread().getContextClassLoader());
      } catch (ClassNotFoundException e) {
        log.warn("Class not found defined in dbObj: ", e);
      }
    }
    return (Class<T>) c;
  }

  public static Type getParameterizedType(final Field field, final int index) {
    if (field != null) {
      if (field.getGenericType() instanceof ParameterizedType) {
        final ParameterizedType type = (ParameterizedType) field.getGenericType();
        if ((type.getActualTypeArguments() != null) && (type.getActualTypeArguments().length <= index)) {
          return null;
        }
        final Type paramType = Objects.requireNonNull(type.getActualTypeArguments())[index];
        if (paramType instanceof GenericArrayType) {
          return paramType; //((GenericArrayType) paramType).getGenericComponentType();
        } else {
          if (paramType instanceof ParameterizedType) {
            return paramType;
          } else {
            if (paramType instanceof TypeVariable) {
              // TODO: Figure out what to do... Walk back up the to
              // the parent class and try to get the variable type
              // from the T/V/X
              // throw new MappingException("Generic Typed Class not supported:  <" + ((TypeVariable)
              // paramType).getName() + "> = " + ((TypeVariable) paramType).getBounds()[0]);
              return paramType;
            } else if (paramType instanceof WildcardType) {
              return paramType;
            } else if (paramType instanceof Class) {
              return paramType;
            } else {
              // Todo(Alexei) add custom exception
              throw new RuntimeException("Unknown type... pretty bad... call for help, wave your hands... yeah!");
            }
          }
        }
      }

      // Not defined on field, but may be on class or super class...
      return getParameterizedClass(field.getType());
    }

    return null;
  }

  public static Class<?> getParameterizedClass(final Class<?> c, final int index) {
    final TypeVariable<?>[] typeVars = c.getTypeParameters();
    if (typeVars.length > 0) {
      final TypeVariable<?> typeVariable = typeVars[index];
      final Type[] bounds = typeVariable.getBounds();

      final Type type = bounds[0];
      if (type instanceof Class) {
        return (Class<?>) type; // broke for EnumSet, cause bounds contain
        // type instead of class
      } else {
        return null;
      }
    } else {
      Type superclass = c.getGenericSuperclass();
      if (superclass == null && c.isInterface()) {
        Type[] interfaces = c.getGenericInterfaces();
        if (interfaces.length > 0) {
          superclass = interfaces[index];
        }
      }
      if (superclass instanceof ParameterizedType) {
        final Type[] actualTypeArguments = ((ParameterizedType) superclass).getActualTypeArguments();
        return actualTypeArguments.length > index ? (Class<?>) actualTypeArguments[index] : null;
      } else if (!Object.class.equals(superclass)) {
        return getParameterizedClass((Class<?>) superclass);
      } else {
        return null;
      }
    }
  }

  public static Class<?> getParameterizedClass(final Class<?> c) {
    return getParameterizedClass(c, 0);
  }

  public static boolean implementsInterface(final Class<?> type, final Class<?> interfaceClass) {
    return interfaceClass.isAssignableFrom(type);
  }

  public static boolean isPrimitiveLike(final Class<?> type) {
    return type != null
        && (type == String.class || type == char.class || type == Character.class || type == short.class
            || type == Short.class || type == Integer.class || type == int.class || type == Long.class
            || type == long.class || type == Double.class || type == double.class || type == float.class
            || type == Float.class || type == Boolean.class || type == boolean.class || type == Byte.class
            || type == byte.class || type == Date.class || type == Locale.class || type == Class.class
            || type == UUID.class || type == URI.class || type.isEnum());
  }

  public static Object convertToArray(final Class<?> type, final List<?> values) {
    final Object exampleArray = Array.newInstance(type, values.size());
    try {
      return values.toArray((Object[]) exampleArray);
    } catch (ClassCastException e) {
      for (int i = 0; i < values.size(); i++) {
        Array.set(exampleArray, i, values.get(i));
      }
      return exampleArray;
    }
  }

  public static <T> List<T> iterToList(final Iterable<T> it) {
    if (it instanceof List) {
      return (List<T>) it;
    }
    if (it == null) {
      return null;
    }

    final List<T> ar = new ArrayList<>();
    for (final T o : it) {
      ar.add(o);
    }

    return ar;
  }

  public static boolean isMap(Document document) {
    return !containsIdentifier(document);
  }

  public static boolean containsIdentifier(Document document) {
    return document.containsKey(Recaster.RECAST_CLASS_KEY);
  }

  public static <T> String obtainRecasterAliasValueOrNull(Class<T> clazz) {
    RecasterAlias recasterAlias = clazz.getAnnotation(RecasterAlias.class);
    if (recasterAlias == null) {
      return null;
    }

    return recasterAlias.value();
  }

  public static <T> void setDocumentIdentifier(Document document, Class<T> clazz) {
    String recasterAliasValue = obtainRecasterAliasValueOrNull(clazz);
    if (recasterAliasValue != null) {
      document.append(Recaster.RECAST_CLASS_KEY, recasterAliasValue);
    } else {
      document.append(Recaster.RECAST_CLASS_KEY, clazz.getName());
    }
  }

  @Nullable
  public static Object getDocumentIdentifier(Document document) {
    return document.get(Recaster.RECAST_CLASS_KEY);
  }
}
