package io.harness.pms.sdk.core.recast;

import static io.harness.pms.sdk.core.recast.RecastReflectionUtils.getParameterizedClass;
import static io.harness.pms.sdk.core.recast.RecastReflectionUtils.implementsInterface;
import static io.harness.pms.sdk.core.recast.RecastReflectionUtils.isPropertyType;

import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.mongodb.morphia.mapping.Mapper;

@Getter
@Slf4j
public class DefaultFieldRecaster implements CustomFieldRecaster {
  private final Recaster recaster;
  private final Transformer transformer;

  public DefaultFieldRecaster(Recaster recaster, Transformer transformer) {
    this.recaster = recaster;
    this.transformer = transformer;
  }

  @Override
  public void fromDocument() {}

  @Override
  public void toDocument(Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    final String name = cf.getNameToStore();

    final Object fieldValue = cf.getFieldValue(entity);

    if (cf.isMap()) {
      writeMap(cf, document, involvedObjects, name, fieldValue);
    } else if (cf.isMultipleValues()) {
      writeCollection(cf, document, involvedObjects, name, fieldValue);
    } else {
      // run converters
      if (this.getTransformer().hasConverter(cf)) {
        transformer.toDocument(entity, cf, document);
        return;
      }

      final Document doc = fieldValue == null ? null : recaster.toDocument(fieldValue, involvedObjects);
      if (doc != null) {
        if (!RecastReflectionUtils.shouldSaveClassName(fieldValue, doc, cf)) {
          doc.remove(Recaster.RECAST_CLASS_KEY);
        }

        if (!doc.keySet().isEmpty()) {
          document.put(name, doc);
        }
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void writeMap(final CastedField cf, final Document document, final Map<Object, Document> involvedObjects,
      final String name, final Object fieldValue) {
    final Map<String, Object> map = (Map<String, Object>) fieldValue;
    if (map != null) {
      final Document mapDoc = new Document();

      for (final Map.Entry<String, Object> entry : map.entrySet()) {
        final Object entryVal = entry.getValue();
        final Object val;

        if (entryVal == null) {
          val = null;
        } else if (transformer.hasConverter(cf) || transformer.hasConverter(entryVal.getClass())) {
          val = transformer.encode(entryVal);
        } else {
          if (Map.class.isAssignableFrom(entryVal.getClass())
              || Collection.class.isAssignableFrom(entryVal.getClass())) {
            val = toComplexDocument(entryVal, true);
          } else {
            val = recaster.toDocument(entryVal, involvedObjects);
          }

          if (!RecastReflectionUtils.shouldSaveClassName(entryVal, val, cf)) {
            if (val instanceof List) {
              if (((List) val).get(0) instanceof Document) {
                List<Document> list = (List<Document>) val;
                for (Document o : list) {
                  o.remove(Mapper.CLASS_NAME_FIELDNAME);
                }
              }
            } else {
              ((Document) val).remove(Mapper.CLASS_NAME_FIELDNAME);
            }
          }
        }

        final String strKey = transformer.encode(entry.getKey()).toString();
        mapDoc.put(strKey, val);
      }

      if (!mapDoc.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        document.append(name, mapDoc);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void writeCollection(final CastedField cf, final Document document,
      final Map<Object, Document> involvedObjects, final String name, final Object fieldValue) {
    Iterable coll = null;

    if (fieldValue != null) {
      if (cf.isArray()) {
        coll = Arrays.asList((Object[]) fieldValue);
      } else {
        coll = (Iterable) fieldValue;
      }
    }

    if (coll != null) {
      final List<Object> values = new ArrayList<Object>();
      for (final Object o : coll) {
        if (null == o) {
          values.add(null);
        } else if (transformer.hasConverter(cf) || transformer.hasConverter(o.getClass())) {
          values.add(transformer.encode(o));
        } else {
          final Object val;
          if (Collection.class.isAssignableFrom(o.getClass()) || Map.class.isAssignableFrom(o.getClass())) {
            val = toComplexDocument(o, true);
          } else {
            val = recaster.toDocument(o, involvedObjects);
          }

          if (!RecastReflectionUtils.shouldSaveClassName(o, val, cf)) {
            ((DBObject) val).removeField(Mapper.CLASS_NAME_FIELDNAME);
          }

          values.add(val);
        }
      }
      if (!values.isEmpty() || recaster.getOptions().isStoreEmpties()) {
        document.append(name, values);
      }
    }
  }
  private Object toComplexDocument(final Object javaObj, final boolean includeClassName) {
    if (javaObj == null) {
      return null;
    }
    Class origClass = javaObj.getClass();

    if (origClass.isAnonymousClass() && origClass.getSuperclass().isEnum()) {
      origClass = origClass.getSuperclass();
    }

    final Object newObj = transformer.encode(origClass, javaObj);
    if (newObj == null) {
      log.warn("converted " + javaObj + " to null");
      return null;
    }
    final Class type = newObj.getClass();
    final boolean bSameType = origClass.equals(type);

    // TODO: think about this logic a bit more.
    // Even if the converter changed it, should it still be processed?
    if (!bSameType && !(Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type))) {
      return newObj;
    } else { // The converter ran, and produced another type, or it is a list/map

      boolean isSingleValue = true;
      boolean isMap = false;
      Class subType = null;

      if (type.isArray() || Map.class.isAssignableFrom(type) || Iterable.class.isAssignableFrom(type)) {
        isSingleValue = false;
        isMap = implementsInterface(type, Map.class);
        // subtype of Long[], List<Long> is Long
        subType = (type.isArray()) ? type.getComponentType() : getParameterizedClass(type, (isMap) ? 1 : 0);
      }

      if (isSingleValue && !isPropertyType(type)) {
        final Document documentObject = recaster.toDocument(newObj);
        if (!includeClassName) {
          documentObject.remove(Recaster.RECAST_CLASS_KEY);
        }
        return documentObject;
      } else if (newObj instanceof Document) {
        return newObj;
      } else if (isMap) {
        if (isPropertyType(subType)) {
          return recaster.toDocument(newObj);
        } else {
          final LinkedHashMap m = new LinkedHashMap();
          for (final Map.Entry e : (Iterable<Map.Entry>) ((Map) newObj).entrySet()) {
            m.put(e.getKey(), toComplexDocument(e.getValue(), includeClassName));
          }

          return m;
        }
        // Set/List but needs elements converted
      } else if (!isSingleValue && !isPropertyType(subType)) {
        final List<Object> values = new BasicDBList();
        if (type.isArray()) {
          for (final Object obj : (Object[]) newObj) {
            values.add(recaster.toDocument(obj));
          }
        } else {
          for (final Object obj : (Iterable) newObj) {
            values.add(recaster.toDocument(obj));
          }
        }

        return values;
      } else {
        return newObj;
      }
    }
  }
}
