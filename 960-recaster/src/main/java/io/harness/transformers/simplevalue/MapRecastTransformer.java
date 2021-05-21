package io.harness.transformers.simplevalue;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CastedField;
import io.harness.exceptions.MapKeyContainsDotException;
import io.harness.transformers.RecastTransformer;
import io.harness.utils.IterationHelper;
import io.harness.utils.RecastReflectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
public class MapRecastTransformer extends RecastTransformer implements SimpleValueTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    if (fromObject == null) {
      return null;
    }

    final Map<Object, Object> values = getRecaster().getObjectFactory().createMap(castedField);
    new IterationHelper<>().loopMap(fromObject, (k, val) -> {
      final Object objKey = getRecaster().getTransformer().decode(
          castedField == null ? k.getClass() : castedField.getMapKeyClass(), k, castedField);
      if (val == null) {
        values.put(objKey, null);
      } else if (val instanceof Document && RecastReflectionUtils.containsIdentifier((Document) val)) {
        values.put(objKey,
            getRecaster().fromDocument(
                (Document) val, (Object) getRecaster().getObjectFactory().createInstance(null, (Document) val)));
      } else {
        values.put(objKey, getRecaster().getTransformer().decode(val.getClass(), val, castedField));
      }
    });

    return values;
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    if (value == null) {
      return null;
    }

    final Map<?, ?> map = (Map<?, ?>) value;
    if (!map.isEmpty() || getRecaster().getOptions().isStoreEmpties()) {
      final Map<String, Object> mapForDb = new LinkedHashMap<>();
      for (final Map.Entry<?, ?> entry : map.entrySet()) {
        final String strKey = getRecaster().getTransformer().encode(entry.getKey()).toString();
        throwIfConstainsDots(strKey);
        if (getRecaster().getTransformer().hasSimpleValueTransformer(entry.getValue())) {
          mapForDb.put(strKey, getRecaster().getTransformer().encode(entry.getValue()));
        } else {
          mapForDb.put(strKey, getRecaster().toDocument(entry.getValue()));
        }
      }
      return new Document(mapForDb);
    }
    return null;
  }

  @Override
  public boolean isSupported(final Class<?> c, final CastedField castedField) {
    if (castedField != null) {
      return castedField.isMap();
    } else {
      return RecastReflectionUtils.implementsInterface(c, Map.class);
    }
  }

  private void throwIfConstainsDots(String key) {
    if (key.contains(".")) {
      throw new MapKeyContainsDotException(String.format("Map key should not contain dots inside -> %s", key));
    }
  }
}
