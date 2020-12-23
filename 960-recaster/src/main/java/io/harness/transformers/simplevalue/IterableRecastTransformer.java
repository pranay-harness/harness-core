package io.harness.transformers.simplevalue;

import static java.lang.String.format;

import io.harness.beans.CastedField;
import io.harness.core.RecastObjectFactory;
import io.harness.core.Transformer;
import io.harness.exceptions.RecasterException;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.RecastReflectionUtils;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.bson.Document;

public class IterableRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  @Override
  @SuppressWarnings("unchecked")
  public Object decode(final Class<?> targetClass, final Object fromDBObject, final CastedField castedField) {
    if (castedField == null || fromDBObject == null) {
      return fromDBObject;
    }

    final Class<?> subtypeDest = castedField.getSubClass();
    final Collection<Object> values = createNewCollection(castedField);

    final Transformer transformer = getRecaster().getTransformer();
    if (fromDBObject.getClass().isArray()) {
      // This should never happen. The driver always returns list/arrays as a List
      for (final Object o : (Object[]) fromDBObject) {
        values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, castedField));
      }
    } else if (fromDBObject instanceof Iterable) {
      // map back to the java data type
      // (List/Set/Array[])
      for (final Object o : (Iterable<Object>) fromDBObject) {
        if (o instanceof Document) {
          values.add(getRecaster().fromDocument(
              (Document) o, (Object) getRecaster().getObjectFactory().createInstance(null, (Document) o)));
        } else {
          values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, castedField));
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

    final List<Object> values = new ArrayList<>();
    if (mf != null && mf.getSubClass() != null) {
      for (final Object o : iterableValues) {
        values.add(getRecaster().getTransformer().encode(mf.getSubClass(), o));
      }
    } else {
      for (final Object o : iterableValues) {
        values.add(getRecaster().getTransformer().encode(o));
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

  private Collection<Object> createNewCollection(final CastedField castedField) {
    final RecastObjectFactory of = getRecaster().getObjectFactory();
    return castedField.isSet() ? of.createSet(castedField) : of.createList(castedField);
  }
}
