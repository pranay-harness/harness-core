package software.wings.stencils;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.collections.MapUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.utils.JsonUtils;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

;

/**
 * Created by peeyushaggarwal on 6/27/16.
 */
@Singleton
public class StencilPostProcessor {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private Injector injector;

  /**
   * Post process list.
   *
   * @param <T>      the type parameter
   * @param stencils the stencils
   * @param appId    the app id
   * @param args     the args
   * @return the list
   */
  public <T extends Stencil> List<Stencil> postProcess(List<T> stencils, String appId, String... args) {
    return stencils.stream().flatMap(t -> processStencil(t, appId, args)).collect(toList());
  }

  private <T extends Stencil> Stream<Stencil> processStencil(T t, String appId, String... args) {
    Stream<Stencil> returnValue;
    if (stream(t.getTypeClass().getDeclaredFields())
            .filter(this ::hasStencilPostProcessAnnotation)
            .findFirst()
            .isPresent()) {
      returnValue =
          stream(t.getTypeClass().getDeclaredFields()).filter(this ::hasStencilPostProcessAnnotation).flatMap(field -> {
            EnumData enumData = field.getAnnotation(EnumData.class);
            Expand expand = field.getAnnotation(Expand.class);
            DefaultValue defaultValue = field.getAnnotation(DefaultValue.class);

            T stencil = t;
            if (enumData != null) {
              DataProvider dataProvider = injector.getInstance(enumData.enumDataProvider());
              Map<String, String> data = dataProvider.getData(appId, args);
              if (!isEmpty(data)) {
                if (enumData.expandIntoMultipleEntries()) {
                  return expandBasedOnEnumData(stencil, data, field.getName());
                } else {
                  stencil = (T) addEnumDataToNode(stencil, data, field.getName());
                }
              }
            }

            if (expand != null) {
              DataProvider dataProvider = injector.getInstance(expand.dataProvider());
              Map<String, String> data = dataProvider.getData(appId, args);
              if (!isEmpty(data)) {
                return expandBasedOnData(stencil, data, field.getName());
              }
            }

            if (defaultValue != null) {
              stencil = (T) addDefaultValueToStencil(stencil, field.getName(), defaultValue.value());
            }
            return Stream.of(stencil);
          });
    } else {
      returnValue = Stream.of(t.getOverridingStencil());
    }

    if (stream(t.getTypeClass().getDeclaredMethods())
            .filter(method -> method.getAnnotation(DefaultValue.class) != null)
            .findFirst()
            .isPresent()) {
      List<Stencil> stencils = returnValue.collect(toList());
      stream(t.getTypeClass().getDeclaredMethods())
          .filter(method -> method.getAnnotation(DefaultValue.class) != null)
          .forEach(method -> {
            DefaultValue defaultValue = method.getAnnotation(DefaultValue.class);
            String fieldName = getField(method);
            if (isNotBlank(fieldName)) {
              List<Stencil> tempStencils =
                  stencils.stream()
                      .map(stencil -> addDefaultValueToStencil(stencil, fieldName, defaultValue.value()))
                      .collect(toList());
              stencils.clear();
              stencils.addAll(tempStencils);
            }
          });
      returnValue = stencils.stream();
    }

    return returnValue;
  }

  private static String getField(Method method) {
    try {
      Class<?> clazz = method.getDeclaringClass();
      BeanInfo info = Introspector.getBeanInfo(clazz);
      PropertyDescriptor[] props = info.getPropertyDescriptors();
      for (PropertyDescriptor pd : props) {
        if (method.equals(pd.getWriteMethod()) || method.equals(pd.getReadMethod())) {
          return pd.getName();
        }
      }
    } catch (IntrospectionException e) {
      e.printStackTrace();
    } catch (Exception e) {
      e.printStackTrace();
    }

    return null;
  }

  private boolean hasStencilPostProcessAnnotation(Field field) {
    return field.getAnnotation(EnumData.class) != null || field.getAnnotation(Expand.class) != null
        || field.getAnnotation(DefaultValue.class) != null;
  }

  private <T extends Stencil> Stencil addDefaultValueToStencil(T stencil, String fieldName, String value) {
    try {
      if (value != null) {
        JsonNode jsonSchema = stencil.getJsonSchema();
        ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(fieldName));
        jsonSchemaField.set("default", JsonUtils.asTree(value));
        OverridingStencil overridingStencil = stencil.getOverridingStencil();
        overridingStencil.setOverridingJsonSchema(jsonSchema);
        return overridingStencil;
      }
    } catch (Exception e) {
      logger.warn("Unable to set default value for stencil {}:field {} with value {}", stencil, fieldName, value);
    }
    return stencil;
  }

  private <T extends Stencil> Stream<Stencil> expandBasedOnEnumData(T t, Map<String, String> data, String fieldName) {
    try {
      if (data != null) {
        return data.keySet().stream().map(key -> {
          JsonNode jsonSchema = t.getJsonSchema();
          ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(fieldName));
          jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
          jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));
          jsonSchemaField.set("default", JsonUtils.asTree(key));
          OverridingStencil overridingStencil = t.getOverridingStencil();
          overridingStencil.setOverridingJsonSchema(jsonSchema);
          overridingStencil.setOverridingName(data.get(key));
          return overridingStencil;
        });
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, fieldName, data);
    }
    return Stream.of(t);
  }

  private <T extends Stencil> Stream<Stencil> expandBasedOnData(T t, Map<String, String> data, String fieldName) {
    try {
      if (data != null) {
        return data.keySet().stream().map(key -> {
          JsonNode jsonSchema = t.getJsonSchema();
          ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(fieldName));
          jsonSchemaField.set("default", JsonUtils.asTree(key));
          OverridingStencil overridingStencil = t.getOverridingStencil();
          overridingStencil.setOverridingJsonSchema(jsonSchema);
          overridingStencil.setOverridingName(data.get(key));
          return overridingStencil;
        });
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, fieldName, data);
    }
    return Stream.of(t);
  }

  private <T extends Stencil> Stencil addEnumDataToNode(T t, Map<String, String> data, String fieldName) {
    try {
      if (data != null) {
        JsonNode jsonSchema = t.getJsonSchema();
        ObjectNode jsonSchemaField = ((ObjectNode) jsonSchema.get("properties").get(fieldName));
        jsonSchemaField.set("enum", JsonUtils.asTree(data.keySet()));
        jsonSchemaField.set("enumNames", JsonUtils.asTree(data.values()));
        OverridingStencil overridingStencil = t.getOverridingStencil();
        overridingStencil.setOverridingJsonSchema(jsonSchema);
        return overridingStencil;
      }
    } catch (Exception e) {
      logger.warn("Unable to fill in values for stencil {}:field {} with data {}", t, fieldName, data);
    }
    return t;
  }
}
