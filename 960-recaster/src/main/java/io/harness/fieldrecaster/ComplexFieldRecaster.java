package io.harness.fieldrecaster;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CastedField;
import io.harness.beans.RecasterMap;
import io.harness.core.Recaster;
import io.harness.exceptions.RecasterException;
import io.harness.utils.RecastReflectionUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@OwnedBy(HarnessTeam.PIPELINE)
@Slf4j
public class ComplexFieldRecaster implements FieldRecaster {
  @Override
  public void fromDocument(Recaster recaster, final Document document, final CastedField cf, final Object entity) {
    try {
      final Object docVal = cf.getDocumentValue(document);
      if (docVal != null) {
        Object refObj;
        if (recaster.getTransformer().hasSimpleValueTransformer(cf.getType())) {
          refObj = recaster.getTransformer().decode(cf.getType(), docVal, cf);
        } else if (!(docVal instanceof Document) && recaster.getTransformer().hasSimpleValueTransformer(docVal)) {
          // special case for parameterized classes. E.x: Dummy<T>
          refObj = recaster.getTransformer().decode(docVal.getClass(), docVal, cf);
        } else {
          Document value = (Document) docVal;
          if (RecastReflectionUtils.isMap(value)) {
            refObj = recaster.getTransformer().decode(LinkedHashMap.class, value, null);
          } else if (recaster.getTransformer().hasCustomTransformer(RecastReflectionUtils.getClass(value))) {
            refObj = recaster.getTransformer().decode(RecastReflectionUtils.getClass(value), value, cf);
          } else {
            refObj = recaster.getObjectFactory().createInstance(recaster, cf, value);
            refObj = recaster.fromDocument(value, refObj);
          }
        }
        if (refObj != null) {
          cf.setFieldValue(entity, refObj);
        }
      }
    } catch (Exception e) {
      throw new RecasterException("Exception while processing complex field", e);
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public void fromMap(Recaster recaster, RecasterMap recasterMap, CastedField cf, Object entity) {
    try {
      final Object docVal = cf.getRecastedMapValue(recasterMap);
      if (docVal != null) {
        Object refObj;
        if (recaster.getTransformer().hasSimpleValueTransformer(cf.getType())) {
          refObj = recaster.getTransformer().decode(cf.getType(), docVal, cf);
        } else if (!(docVal instanceof Map) && recaster.getTransformer().hasSimpleValueTransformer(docVal)) {
          // special case for parameterized classes. E.x: Dummy<T>
          refObj = recaster.getTransformer().decode(docVal.getClass(), docVal, cf);
        } else {
          RecasterMap value = RecasterMap.cast((Map<String, Object>) docVal);
          if (!value.containsIdentifier()) {
            refObj = recaster.getTransformer().decode(LinkedHashMap.class, value, null);
          } else if (recaster.getTransformer().hasCustomTransformer(RecastReflectionUtils.getClass(value))) {
            refObj = recaster.getTransformer().decode(RecastReflectionUtils.getClass(value), value, cf);
          } else {
            refObj = recaster.getObjectFactory().createInstance(recaster, cf, value);
            refObj = recaster.fromMap(value, refObj);
          }
        }
        if (refObj != null) {
          cf.setFieldValue(entity, refObj);
        }
      }
    } catch (Exception e) {
      throw new RecasterException("Exception while processing complex field", e);
    }
  }

  @Override
  public void toDocument(
      Recaster recaster, Object entity, CastedField cf, Document document, Map<Object, Document> involvedObjects) {
    final String name = cf.getNameToStore();

    final Object fieldValue = cf.getFieldValue(entity);

    if (recaster.getTransformer().hasSimpleValueTransformer(fieldValue)) {
      recaster.getTransformer().toDocument(entity, cf, document);
      return;
    }

    if (recaster.getTransformer().hasCustomTransformer(cf.getType())) {
      document.put(cf.getNameToStore(), obtainEncodedValue(recaster, cf, fieldValue));
      return;
    }

    final Document doc = fieldValue == null ? null : recaster.toDocument(fieldValue, involvedObjects);
    if (doc != null && !doc.keySet().isEmpty()) {
      document.put(name, doc);
    }
  }

  @Override
  public void toMap(Recaster recaster, Object entity, CastedField cf, RecasterMap recasterMap) {
    final String name = cf.getNameToStore();

    final Object fieldValue = cf.getFieldValue(entity);

    if (recaster.getTransformer().hasSimpleValueTransformer(fieldValue)) {
      recaster.getTransformer().putToMap(entity, cf, recasterMap);
      return;
    }

    if (recaster.getTransformer().hasCustomTransformer(cf.getType())) {
      recasterMap.append(cf.getNameToStore(), obtainEncodedValueInternal(recaster, cf, fieldValue));
      return;
    }

    final Map<String, Object> map = fieldValue == null ? null : recaster.toMap(fieldValue);
    if (map != null && !map.keySet().isEmpty()) {
      recasterMap.append(name, map);
    }
  }

  private Document obtainEncodedValue(Recaster recaster, CastedField cf, Object fieldValue) {
    Document document = new Document();
    RecastReflectionUtils.setDocumentIdentifier(document, cf.getType());
    document.append(Recaster.ENCODED_VALUE, recaster.getTransformer().encode(cf.getType(), fieldValue, cf));
    return document;
  }

  private Map<String, Object> obtainEncodedValueInternal(Recaster recaster, CastedField cf, Object fieldValue) {
    Map<String, Object> recastedMap = new LinkedHashMap<>();
    RecastReflectionUtils.setIdentifier(recastedMap, cf.getType());
    recastedMap.put(Recaster.ENCODED_VALUE, recaster.getTransformer().encode(cf.getType(), fieldValue, cf));
    return recastedMap;
  }
}
