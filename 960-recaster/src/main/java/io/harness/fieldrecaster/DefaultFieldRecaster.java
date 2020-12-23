package io.harness.fieldrecaster;

import io.harness.beans.CastedField;
import io.harness.beans.EphemeralCastedField;
import io.harness.core.Recaster;
import io.harness.utils.IterationHelper;
import io.harness.utils.RecastReflectionUtils;

import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class DefaultFieldRecaster implements FieldRecaster {
  @Override
  public void fromDocument(Recaster recaster, final Document document, final CastedField cf, final Object entity) {
    try {
      if (cf.isMap()) {
        readMap(recaster, entity, cf, document);
      } else if (cf.isMultipleValues()) {
        readCollection(recaster, entity, cf, document);
      } else {
        final Object docVal = cf.getDocumentValue(document);
        if (docVal != null) {
          Object refObj;
          if (recaster.getTransformer().hasSimpleValueTransformer(cf)
              || recaster.getTransformer().hasSimpleValueTransformer(cf.getType())) {
            refObj = recaster.getTransformer().decode(cf.getType(), docVal, cf);
          } else {
            Document value = (Document) docVal;
            refObj = recaster.getObjectFactory().createInstance(recaster, cf, value);
            refObj = recaster.fromDocument(value, refObj);
          }
          if (refObj != null) {
            cf.setFieldValue(entity, refObj);
          }
        }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void toDocument(
      Recaster recaster, Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    final String name = cf.getNameToStore();

    final Object fieldValue = cf.getFieldValue(entity);

    if (cf.isMap()) {
      writeMap(recaster, cf, document, involvedObjects, name, fieldValue);
    } else if (cf.isMultipleValues()) {
      writeCollection(recaster, cf, document, involvedObjects, name, fieldValue);
    } else {
      // run converters
      if (recaster.getTransformer().hasSimpleValueTransformer(cf)) {
        recaster.getTransformer().toDocument(entity, cf, document);
        return;
      }

      final Document doc = fieldValue == null ? null : recaster.toDocument(fieldValue, involvedObjects);
      if (doc != null) {
        if (!doc.keySet().isEmpty()) {
          document.put(name, doc);
        }
      }
    }
  }

  private void readMap(Recaster recaster, Object entity, CastedField cf, Document document) {
    final Document docObj = (Document) cf.getDocumentValue(document);

    if (docObj != null) {
      final Map<Object, Object> map = recaster.getObjectFactory().createMap(cf);

      final EphemeralCastedField ephemeralCastedField =
          isMapOrCollection(cf) ? new EphemeralCastedField((ParameterizedType) cf.getSubType(), cf, recaster) : null;
      new IterationHelper<>().loopMap(docObj, (k, val) -> {
        Object newEntity = null;

        // run converters
        if (val != null) {
          if (recaster.getTransformer().hasSimpleValueTransformer(cf)
              || recaster.getTransformer().hasSimpleValueTransformer(cf.getSubClass())) {
            newEntity = recaster.getTransformer().decode(cf.getSubClass(), val, cf);
          } else {
            if (val instanceof Document) {
              newEntity = readMapOrCollectionOrEntity(recaster, cf, ephemeralCastedField, (Document) val);
            } else if (Map.class.isAssignableFrom(val.getClass())
                || Collection.class.isAssignableFrom(val.getClass())) {
              newEntity = fromComplexObject(recaster, val, cf);
            } else {
              newEntity = val;
            }
          }
        }

        final Object objKey = recaster.getTransformer().decode(cf.getMapKeyClass(), k, cf);
        map.put(objKey, newEntity);
      });

      if (!map.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        cf.setFieldValue(entity, map);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void readCollection(Recaster recaster, Object entity, CastedField cf, Document document) {
    Collection<Object> values;

    final Object docVal = cf.getDocumentValue(document);
    if (docVal != null) {
      // multiple documents in a List
      values = cf.isSet() ? recaster.getObjectFactory().createSet(cf) : recaster.getObjectFactory().createList(cf);

      final List<Object> dbValues;
      if (docVal instanceof List) {
        dbValues = (List<Object>) docVal;
      } else {
        dbValues = new ArrayList<>();
        dbValues.add(docVal);
      }

      EphemeralCastedField ephemeralCastedField =
          !recaster.isCasted(cf.getType()) && isMapOrCollection(cf) && (cf.getSubType() instanceof ParameterizedType)
          ? new EphemeralCastedField((ParameterizedType) cf.getSubType(), cf, recaster)
          : null;
      for (final Object o : dbValues) {
        Object newEntity = null;

        if (o != null) {
          // run converters
          if (recaster.getTransformer().hasSimpleValueTransformer(cf)
              || recaster.getTransformer().hasSimpleValueTransformer(cf.getSubClass())) {
            newEntity = recaster.getTransformer().decode(cf.getSubClass(), o, cf);
          } else {
            if (o instanceof Document) {
              newEntity = readMapOrCollectionOrEntity(recaster, cf, ephemeralCastedField, (Document) o);
            } else if (Map.class.isAssignableFrom(o.getClass()) || Collection.class.isAssignableFrom(o.getClass())) {
              newEntity = fromComplexObject(recaster, o, cf);
            } else {
              newEntity = o;
            }
          }
        }

        values.add(newEntity);
      }
      if (!values.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        if (cf.getType().isArray()) {
          cf.setFieldValue(
              entity, RecastReflectionUtils.convertToArray(cf.getSubClass(), RecastReflectionUtils.iterToList(values)));
        } else {
          cf.setFieldValue(entity, values);
        }
      }
    }
  }

  private static boolean isMapOrCollection(final CastedField cf) {
    return Map.class.isAssignableFrom(cf.getSubClass()) || Iterable.class.isAssignableFrom(cf.getSubClass());
  }

  private Object readMapOrCollectionOrEntity(final Recaster recaster, final CastedField cf,
      final EphemeralCastedField ephemeralCastedField, final Document document) {
    if (ephemeralCastedField != null) {
      recaster.fromDocument(document, ephemeralCastedField);
      return ephemeralCastedField.getValue();
    } else {
      final Object newEntity = recaster.getObjectFactory().createInstance(recaster, cf, document);
      return recaster.fromDocument(document, newEntity);
    }
  }

  @SuppressWarnings("unchecked")
  private void writeMap(final Recaster recaster, final CastedField cf, final Document document,
      final Map<Object, Document> involvedObjects, final String name, final Object fieldValue) {
    final Map<String, Object> map = (Map<String, Object>) fieldValue;
    // TODO(Alexei) Fast return
    if (map != null) {
      final Document mapDoc = new Document();

      for (final Map.Entry<String, Object> entry : map.entrySet()) {
        final Object entryVal = entry.getValue();
        final Object val;

        if (entryVal == null) {
          val = null;
        } else if (recaster.getTransformer().hasSimpleValueTransformer(cf)
            || recaster.getTransformer().hasSimpleValueTransformer(entryVal.getClass())) {
          val = recaster.getTransformer().encode(entryVal);
        } else {
          if (Map.class.isAssignableFrom(entryVal.getClass())
              || Collection.class.isAssignableFrom(entryVal.getClass())) {
            val = toComplexObject(recaster, entryVal, true);
          } else {
            val = recaster.toDocument(entryVal, involvedObjects);
          }
        }

        final String strKey = recaster.getTransformer().encode(entry.getKey()).toString();
        mapDoc.put(strKey, val);
      }

      if (!mapDoc.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        document.append(name, mapDoc);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void writeCollection(final Recaster recaster, final CastedField cf, final Document document,
      final Map<Object, Document> involvedObjects, final String name, final Object fieldValue) {
    Iterable<Object> coll = null;

    if (fieldValue != null) {
      if (cf.isArray()) {
        coll = Arrays.asList((Object[]) fieldValue);
      } else {
        coll = (Iterable<Object>) fieldValue;
      }
    }

    if (coll != null) {
      final List<Object> values = new ArrayList<>();
      for (final Object o : coll) {
        if (null == o) {
          values.add(null);
        } else if (recaster.getTransformer().hasSimpleValueTransformer(cf)
            || recaster.getTransformer().hasSimpleValueTransformer(o.getClass())) {
          values.add(recaster.getTransformer().encode(o));
        } else {
          final Object val;
          if (Collection.class.isAssignableFrom(o.getClass()) || Map.class.isAssignableFrom(o.getClass())) {
            val = toComplexObject(recaster, o, true);
          } else {
            val = recaster.toDocument(o, involvedObjects);
          }

          values.add(val);
        }
      }
      if (!values.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        document.append(name, values);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private Object toComplexObject(final Recaster recaster, final Object javaObj, final boolean includeClassName) {
    if (javaObj == null) {
      return null;
    }
    Class<?> origClass = javaObj.getClass();

    if (origClass.isAnonymousClass() && origClass.getSuperclass().isEnum()) {
      origClass = origClass.getSuperclass();
    }

    final Object newObj = recaster.getTransformer().encode(origClass, javaObj);
    if (newObj == null) {
      log.warn("converted " + javaObj + " to null");
      return null;
    }
    final Class<?> type = newObj.getClass();
    final boolean bSameType = origClass.equals(type);

    // TODO: think about this logic a bit more.
    // Even if the converter changed it, should it still be processed?
    if (!bSameType && !(Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type))) {
      return newObj;
    } else { // The converter ran, and produced another type, or it is a list/map

      boolean isSingleValue = true;
      boolean isMap = false;
      Class<?> subType = null;

      if (type.isArray() || Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type)) {
        isSingleValue = false;
        isMap = RecastReflectionUtils.implementsInterface(type, Map.class);
        // subtype of Long[], List<Long> is Long
        subType =
            type.isArray() ? type.getComponentType() : RecastReflectionUtils.getParameterizedClass(type, isMap ? 1 : 0);
      }

      if (isSingleValue && !RecastReflectionUtils.isPrimitiveLike(type)) {
        final Document documentObject = recaster.toDocument(newObj);
        if (!includeClassName) {
          documentObject.remove(Recaster.RECAST_CLASS_KEY);
        }
        return documentObject;
      } else if (newObj instanceof Document) {
        return newObj;
      } else if (isMap) {
        if (RecastReflectionUtils.isPrimitiveLike(subType)) {
          return recaster.toDocument(newObj);
        } else {
          final LinkedHashMap<Object, Object> m = new LinkedHashMap<>();
          for (final Map.Entry<Object, Object> e : ((Map<Object, Object>) newObj).entrySet()) {
            m.put(e.getKey(), toComplexObject(recaster, e.getValue(), includeClassName));
          }

          return m;
        }
        // Set/List but needs elements converted
      } else if (!isSingleValue && !RecastReflectionUtils.isPrimitiveLike(subType)) {
        final List<Object> values = new ArrayList<>();
        if (type.isArray()) {
          for (final Object obj : (Object[]) newObj) {
            values.add(recaster.toDocument(obj));
          }
        } else {
          for (final Object obj : (Iterable<Object>) newObj) {
            values.add(recaster.toDocument(obj));
          }
        }

        return values;
      } else {
        return newObj;
      }
    }
  }

  private Object fromComplexObject(final Recaster recaster, final Object complexObject, final CastedField cf) {
    if (complexObject == null) {
      return null;
    }

    Class<?> origClass = complexObject.getClass();

    if (origClass.isAnonymousClass() && origClass.getSuperclass().isEnum()) {
      origClass = origClass.getSuperclass();
    }

    final Object newObj = recaster.getTransformer().decode(origClass, complexObject, cf);
    if (newObj == null) {
      log.warn("converted " + complexObject + " to null");
      return null;
    }
    final Class<?> type = newObj.getClass();
    final boolean bSameType = origClass.equals(type);

    if (!bSameType && !(Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type))) {
      return newObj;
    } else { // The converter ran, and produced another type, or it is a list/map

      boolean isSingleValue = true;
      boolean isMap = false;
      Class<?> subType = null;

      if (type.isArray() || Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type)) {
        isSingleValue = false;
        isMap = RecastReflectionUtils.implementsInterface(type, Map.class);
        // subtype of Long[], List<Long> is Long
        subType =
            type.isArray() ? type.getComponentType() : RecastReflectionUtils.getParameterizedClass(type, isMap ? 1 : 0);
      }

      if (newObj instanceof Document) {
        recaster.fromDocument((Document) newObj, subType);
        return newObj;
      } else if (isMap) {
        // for simple types
        if (RecastReflectionUtils.isPrimitiveLike(subType)) {
          final LinkedHashMap<Object, Object> m = new LinkedHashMap<>();
          for (final Map.Entry<Object, Object> e : ((Map<Object, Object>) newObj).entrySet()) {
            m.put(e.getKey(), recaster.getTransformer().decode(subType, e.getValue(), cf));
          }

          return m;
        } else {
          final LinkedHashMap<Object, Object> m = new LinkedHashMap<>();
          for (final Map.Entry<Object, Object> e : ((Map<Object, Object>) newObj).entrySet()) {
            m.put(e.getKey(), fromComplexObject(recaster, e.getValue(), cf));
          }

          return m;
        }
      } else if (!isSingleValue && !RecastReflectionUtils.isPrimitiveLike(subType)) {
        final List<Object> values = new ArrayList<>();
        if (type.isArray()) {
          for (final Object obj : (Object[]) newObj) {
            if (obj instanceof Document) {
              values.add(recaster.fromDocument((Document) obj, getDocumentClass(recaster, (Document) obj)));
            } else {
              values.add(obj);
            }
          }
        } else {
          for (final Object obj : (Iterable<?>) newObj) {
            if (obj instanceof Document) {
              values.add(recaster.fromDocument((Document) obj, getDocumentClass(recaster, (Document) obj)));
            } else {
              values.add(obj);
            }
          }
        }

        return values;
      } else {
        return newObj;
      }
    }
  }

  private Class<?> getDocumentClass(Recaster recaster, Document document) {
    return recaster.getObjectFactory().createInstance(null, document);
  }
}
