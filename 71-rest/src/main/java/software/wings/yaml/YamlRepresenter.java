package software.wings.yaml;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;

import com.google.common.collect.Lists;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Representer;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;
public class YamlRepresenter extends Representer {
  private boolean removeEmptyValues;

  public YamlRepresenter(boolean removeEmptyValues) {
    this.removeEmptyValues = removeEmptyValues;
  }

  @Override
  public NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
    Class aClass = javaBean.getClass();

    List<Field> fieldList = Lists.newArrayList();
    getFieldsOfClassHierarchy(aClass, fieldList);

    Set<String> yamlSerializableFields = new HashSet<>();

    for (Field field : fieldList) {
      YamlDoNotSerialize doNotAnnos = field.getAnnotation(YamlDoNotSerialize.class);
      Transient transientAnnos = field.getAnnotation(Transient.class);

      if (doNotAnnos == null && transientAnnos == null) {
        yamlSerializableFields.add(field.getName());
      }
    }

    if (removeEmptyValues) {
      if (propertyValue == null || propertyValue.equals("")
          || (propertyValue instanceof Collection<?> && isEmpty((Collection) propertyValue))) {
        return null;
      }
    }

    if (yamlSerializableFields.contains(property.getName())) {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }

    return null;
  }

  public static void getFieldsOfClassHierarchy(@Nonnull Class<?> currentClass, List<Field> fieldList) {
    fieldList.addAll(asList(currentClass.getDeclaredFields()));

    Class<?> parentClass = currentClass.getSuperclass();
    if (parentClass != null) {
      getFieldsOfClassHierarchy(parentClass, fieldList);
    }
  }
}