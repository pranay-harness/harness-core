package io.harness.pms.yaml;

import io.harness.beans.CastedField;
import io.harness.core.Recaster;
import io.harness.exception.InvalidRequestException;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.pms.yaml.ParameterDocumentField.ParameterDocumentFieldKeys;

import java.lang.reflect.Type;
import java.util.Optional;
import lombok.experimental.UtilityClass;
import org.bson.Document;
import sun.reflect.generics.reflectiveObjects.ParameterizedTypeImpl;

@UtilityClass
public class ParameterDocumentFieldMapper {
  public ParameterDocumentField fromParameterField(ParameterField<?> parameterField, CastedField castedField) {
    Class<?> cls = findValueClass(null, castedField);
    if (parameterField == null) {
      if (cls == null) {
        throw new InvalidRequestException("Parameter field is null");
      }
      return ParameterDocumentField.builder()
          .valueDoc(RecastOrchestrationUtils.toDocument(new ParameterFieldValueWrapper<>(null)))
          .valueClass(cls.getSimpleName())
          .typeString(cls.isAssignableFrom(String.class))
          .build();
    }
    return ParameterDocumentField.builder()
        .expression(parameterField.isExpression())
        .expressionValue(parameterField.getExpressionValue())
        .valueDoc(RecastOrchestrationUtils.toDocument(new ParameterFieldValueWrapper<>(parameterField.getValue())))
        .valueClass(cls == null ? null : cls.getSimpleName())
        .inputSetValidator(parameterField.getInputSetValidator())
        .typeString(parameterField.isTypeString())
        .jsonResponseField(parameterField.isJsonResponseField())
        .responseField(parameterField.getResponseField())
        .build();
  }

  public ParameterField<?> toParameterField(ParameterDocumentField documentField) {
    if (documentField == null) {
      return null;
    }
    ParameterFieldValueWrapper<?> parameterFieldValueWrapper =
        RecastOrchestrationUtils.fromDocument(documentField.getValueDoc(), ParameterFieldValueWrapper.class);
    return ParameterField.builder()
        .expression(documentField.isExpression())
        .expressionValue(documentField.getExpressionValue())
        .value(parameterFieldValueWrapper == null ? null : parameterFieldValueWrapper.getValue())
        .inputSetValidator(documentField.getInputSetValidator())
        .typeString(documentField.isTypeString())
        .jsonResponseField(documentField.isJsonResponseField())
        .responseField(documentField.getResponseField())
        .build();
  }

  public Optional<ParameterDocumentField> fromParameterFieldDocument(Object o) {
    if (!(o instanceof Document)) {
      return Optional.empty();
    }

    Document doc = (Document) o;
    String recastClass = (String) doc.getOrDefault(Recaster.RECAST_CLASS_KEY, "");
    if (!recastClass.equals(ParameterField.class.getName())) {
      return Optional.empty();
    }

    Document encodedValue = (Document) RecastOrchestrationUtils.getEncodedValue(doc);
    return Optional.ofNullable(fromDocument(encodedValue));
  }

  public ParameterDocumentField fromDocument(Document doc) {
    if (doc == null) {
      return null;
    }

    // Temporarily store valueDoc so that recaster doesn't deserialize it
    Document valueDoc = (Document) doc.get(ParameterDocumentFieldKeys.valueDoc);
    doc.put(ParameterDocumentFieldKeys.valueDoc, null);

    ParameterDocumentField docField = RecastOrchestrationUtils.fromDocument(doc, ParameterDocumentField.class);
    if (docField == null) {
      return null;
    }

    // Reset valueDoc
    doc.put(ParameterDocumentFieldKeys.valueDoc, valueDoc);
    docField.setValueDoc(valueDoc);
    return docField;
  }

  private Class<?> findValueClass(ParameterField<?> parameterField, CastedField castedField) {
    if (parameterField != null && parameterField.getValue() != null) {
      return parameterField.getValue().getClass();
    }
    if (castedField == null || !(castedField.getGenericType() instanceof ParameterizedTypeImpl)) {
      return null;
    }
    return getFinalClass(((ParameterizedTypeImpl) castedField.getGenericType()).getActualTypeArguments()[0]);
  }

  private Class<?> getFinalClass(Type type) {
    if (type instanceof Class<?>) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedTypeImpl) {
      return ((ParameterizedTypeImpl) type).getRawType();
    }
    return null;
  }
}
