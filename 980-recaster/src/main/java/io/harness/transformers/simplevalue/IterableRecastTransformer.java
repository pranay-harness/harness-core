/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.transformers.simplevalue;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Transformer;
import io.harness.exceptions.RecasterException;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OwnedBy(HarnessTeam.PIPELINE)
public class IterableRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  @Override
  public Object decode(final Class<?> targetClass, final Object fromDBObject, final CastedField castedField) {
    return decodeInternal(targetClass, fromDBObject, castedField);
  }

  @SuppressWarnings("unchecked")
  private Object decodeInternal(final Class<?> targetClass, final Object fromDBObject, final CastedField castedField) {
    if (castedField == null || fromDBObject == null) {
      return fromDBObject;
    }

    Collection<Object> values = createNewCollection(targetClass);

    final Class<?> subtypeDest = castedField.getSubClass();

    final Transformer transformer = getRecaster().getTransformer();
    if (fromDBObject.getClass().isArray()) {
      // This should never happen. The driver always returns list/arrays as a List
      for (final Object o : (Object[]) fromDBObject) {
        values.add(transformer.decode(o.getClass(), o, castedField));
      }
    } else if (fromDBObject instanceof Iterable) {
      for (final Object o : (Iterable<Object>) fromDBObject) {
        if (o instanceof Map) {
          // if document does not have __recast then use HashMap.class to create instance
          RecasterMap recasterMap = new RecasterMap((Map<String, ?>) o);
          values.add(getRecaster().fromMap(
              recasterMap, (Object) getRecaster().getObjectFactory().createInstance(HashMap.class, recasterMap)));
        } else if (o instanceof Iterable) {
          values.add(decodeInternal(o.getClass(), o, castedField));
        } else {
          values.add(transformer.decode(getRealTypeFromCastedField(castedField), o, castedField));
        }
      }
    } else {
      // Single value case.
      values.add(transformer.decode((subtypeDest != null && !Iterable.class.isAssignableFrom(subtypeDest))
              ? subtypeDest
              : fromDBObject.getClass(),
          fromDBObject, castedField));
    }

    // convert to and array if that is the destination type (not a list/set)
    if (castedField.getType().isArray()) {
      return RecastReflectionUtils.convertToArray(subtypeDest, (List<?>) values);
    } else {
      return values;
    }
  }

  private Class<?> getRealTypeFromCastedField(CastedField castedField) {
    CastedField currentCastedField = castedField;
    while (!currentCastedField.getTypeParameters().isEmpty()) {
      if (currentCastedField.isMap()) {
        currentCastedField = currentCastedField.getTypeParameters().get(1);
      } else {
        currentCastedField = currentCastedField.getTypeParameters().get(0);
      }
    }

    return currentCastedField.getRealType();
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object encode(final Object value, final CastedField mf) {
    if (value == null) {
      return null;
    }

    final Iterable<?> iterableValues;

    if (value.getClass().isArray()) {
      if (Array.getLength(value) == 0 || value.getClass().getComponentType().isPrimitive()) {
        return value;
      }

      iterableValues = Arrays.asList((Object[]) value);
    } else {
      if (!(value instanceof Iterable)) {
        throw new RecasterException(format("Cannot cast %s to Iterable for MappedField: %s", value.getClass(), mf));
      }

      // cast value to a common interface
      iterableValues = (Iterable<?>) value;
    }

    final Collection<Object> values = createNewCollection(value.getClass());
    if (mf != null && mf.getSubClass() != null) {
      for (final Object o : iterableValues) {
        if (getRecaster().getTransformer().hasSimpleValueTransformer(mf.getSubClass())) {
          values.add(getRecaster().getTransformer().encode(mf.getSubClass(), o));
        } else if (o instanceof Iterable) {
          encode(o, mf);
        } else {
          values.add(getRecaster().toMap(o));
        }
      }
    } else {
      for (final Object o : iterableValues) {
        if (o != null && getRecaster().getTransformer().hasSimpleValueTransformer(o.getClass())) {
          values.add(getRecaster().getTransformer().encode(o));
        } else if (o instanceof Iterable) {
          encode(o, mf);
        } else {
          values.add(getRecaster().toMap(o));
        }
      }
    }
    return values;
  }

  @Override
  public boolean isSupported(final Class<?> c, final CastedField cf) {
    if (cf != null) {
      return cf.isMultipleValues() && !cf.isMap();
    } else {
      return c.isArray() || RecastReflectionUtils.implementsInterface(c, Iterable.class);
    }
  }

  @SuppressWarnings("unchecked")
  private Collection<Object> createNewCollection(final Class<?> collectionClass) {
    return (Collection<Object>) getRecaster().getObjectFactory().createInstance(collectionClass);
  }
}
