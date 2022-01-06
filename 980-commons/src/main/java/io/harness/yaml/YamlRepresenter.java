/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.dataformat.yaml.snakeyaml.introspector.Property;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.NodeTuple;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.nodes.Tag;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.representer.Representer;
import com.google.common.collect.Lists;
import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

@OwnedBy(CDP)
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
    Set<String> keepEmptyAsIsFields = new HashSet<>();

    for (Field field : fieldList) {
      YamlDoNotSerialize doNotAnnos = field.getAnnotation(YamlDoNotSerialize.class);
      Transient transientAnnos = field.getAnnotation(Transient.class);
      YamlKeepEmptyAsIs keepEmptyAsIs = field.getAnnotation(YamlKeepEmptyAsIs.class);

      if (doNotAnnos == null && transientAnnos == null) {
        yamlSerializableFields.add(field.getName());
      }
      if (keepEmptyAsIs != null) {
        keepEmptyAsIsFields.add(field.getName());
      }
    }

    if (removeEmptyValues) {
      if (propertyValue == null || emptyAndShouldBeRemoved(property, propertyValue, keepEmptyAsIsFields)
          || isEmptyCollection(propertyValue)) {
        return null;
      }
    }

    if (yamlSerializableFields.contains(property.getName())) {
      return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
    }

    return null;
  }

  private boolean emptyAndShouldBeRemoved(Property property, Object propertyValue, Set<String> keepEmptyAsIsFields) {
    return propertyValue.equals("") && !keepEmptyAsIsFields.contains(property.getName());
  }

  private boolean isEmptyCollection(Object propertyValue) {
    return propertyValue instanceof Collection<?> && isEmpty((Collection) propertyValue);
  }

  public static void getFieldsOfClassHierarchy(@Nonnull Class<?> currentClass, List<Field> fieldList) {
    fieldList.addAll(asList(currentClass.getDeclaredFields()));

    Class<?> parentClass = currentClass.getSuperclass();
    if (parentClass != null) {
      getFieldsOfClassHierarchy(parentClass, fieldList);
    }
  }
}
