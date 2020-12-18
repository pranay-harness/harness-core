package io.harness.pms.sdk.core.recast.converters;

import static java.lang.String.format;

import io.harness.pms.sdk.core.recast.CastedField;
import io.harness.pms.sdk.core.recast.EphemeralCastedField;
import io.harness.pms.sdk.core.recast.RecastConverter;
import io.harness.pms.sdk.core.recast.RecastObjectFactory;
import io.harness.pms.sdk.core.recast.Transformer;

import com.mongodb.DBObject;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.mongodb.morphia.converters.ConverterException;
import org.mongodb.morphia.utils.ReflectionUtils;

/**
 * @author Uwe Schaefer, (us@thomas-daily.de)
 * @author scotthernandez
 */
public class IterableRecastConverter extends RecastConverter {
  @Override
  @SuppressWarnings("unchecked")
  public Object decode(final Class targetClass, final Object fromDBObject, final CastedField castedField) {
    if (castedField == null || fromDBObject == null) {
      return fromDBObject;
    }

    final Class subtypeDest = castedField.getSubClass();
    final Collection values = createNewCollection(castedField);

    final Transformer transformer = getRecaster().getTransformer();
    if (fromDBObject.getClass().isArray()) {
      // This should never happen. The driver always returns list/arrays as a List
      for (final Object o : (Object[]) fromDBObject) {
        values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, castedField));
      }
    } else if (fromDBObject instanceof Iterable) {
      // map back to the java data type
      // (List/Set/Array[])
      for (final Object o : (Iterable) fromDBObject) {
        if (o instanceof DBObject) {
          final List<CastedField> typeParameters = castedField.getTypeParameters();
          if (!typeParameters.isEmpty()) {
            final CastedField cf = typeParameters.get(0);
            if (cf instanceof EphemeralCastedField) {
              values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, cf));
            } else {
              throw new UnsupportedOperationException("mappedField isn't an EphemeralMappedField");
            }
          } else {
            values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, castedField));
          }
        } else {
          values.add(transformer.decode((subtypeDest != null) ? subtypeDest : o.getClass(), o, castedField));
        }
      }
    } else {
      // Single value case.
      values.add(
          transformer.decode((subtypeDest != null) ? subtypeDest : fromDBObject.getClass(), fromDBObject, castedField));
    }

    // convert to and array if that is the destination type (not a list/set)
    if (castedField.getType().isArray()) {
      return ReflectionUtils.convertToArray(subtypeDest, (List) values);
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
        throw new ConverterException(format("Cannot cast %s to Iterable for MappedField: %s", value.getClass(), mf));
      }

      // cast value to a common interface
      iterableValues = (Iterable<?>) value;
    }

    final List values = new ArrayList();
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
  protected boolean isSupported(final Class c, final CastedField cf) {
    if (cf != null) {
      return cf.isMultipleValues() && !cf.isMap();
    } else {
      return c.isArray() || ReflectionUtils.implementsInterface(c, Iterable.class);
    }
  }

  private Collection<?> createNewCollection(final CastedField castedField) {
    final RecastObjectFactory of = getRecaster().getObjectFactory();
    return castedField.isSet() ? of.createSet(castedField) : of.createList(castedField);
  }
}
