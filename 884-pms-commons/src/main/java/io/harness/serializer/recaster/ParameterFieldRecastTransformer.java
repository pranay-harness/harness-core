package io.harness.serializer.recaster;

import io.harness.beans.CastedField;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterDocumentField;
import io.harness.pms.yaml.ParameterDocumentFieldMapper;
import io.harness.pms.yaml.ParameterField;
import io.harness.transformers.RecastTransformer;
import io.harness.transformers.simplevalue.CustomValueTransformer;
import io.harness.utils.RecastReflectionUtils;

import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

@Slf4j
public class ParameterFieldRecastTransformer extends RecastTransformer implements CustomValueTransformer {
  @Override
  public Object decode(Class<?> targetClass, Object fromObject, CastedField castedField) {
    try {
      if (fromObject == null) {
        return null;
      }

      ParameterDocumentField documentField =
          RecastOrchestrationUtils.fromDocument((Document) fromObject, ParameterDocumentField.class);
      return ParameterDocumentFieldMapper.toParameterField(documentField);
    } catch (Exception e) {
      log.error("Exception while decoding ParameterField {}", fromObject, e);
      throw e;
    }
  }

  @Override
  public Object encode(Object value, CastedField castedField) {
    try {
      ParameterDocumentField documentField =
          ParameterDocumentFieldMapper.fromParameterField((ParameterField<?>) value, castedField);
      return RecastOrchestrationUtils.toDocument(documentField);
    } catch (Exception e) {
      log.error("Exception while encoding ParameterField {}", value, e);
      throw e;
    }
  }

  @Override
  public boolean isSupported(Class<?> c, CastedField cf) {
    return RecastReflectionUtils.implementsInterface(c, ParameterField.class);
  }
}
