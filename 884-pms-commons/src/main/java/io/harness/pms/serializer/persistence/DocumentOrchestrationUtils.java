package io.harness.pms.serializer.persistence;

import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.serializer.json.JsonOrchestrationUtils;
import io.harness.pms.serializer.json.JsonSerializable;

import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.json.JsonWriterSettings;

@UtilityClass
@Slf4j
public class DocumentOrchestrationUtils {
  public final String PMS_CLASS_KEY = "_pms_class";

  public <T extends JsonSerializable> Document convertToDocument(T value) {
    if (value == null) {
      return null;
    }
    Document document = Document.parse(value.toJson());
    document.put(PMS_CLASS_KEY, value.getClass().getName());
    return document;
  }

  public <T extends JsonSerializable> String convertToDocumentJson(T value) {
    Document document = convertToDocument(value);
    return document == null ? null : document.toJson();
  }

  @SneakyThrows
  public <T extends JsonSerializable> T convertFromDocument(Document value) {
    if (value == null) {
      return null;
    }
    JsonWriterSettings writerSettings =
        JsonWriterSettings.builder().int64Converter((v, writer) -> writer.writeNumber(v.toString())).build();
    Class<?> aClass = Class.forName((String) value.remove(PMS_CLASS_KEY));
    return (T) JsonOrchestrationUtils.asObject(value.toJson(writerSettings), aClass);
  }

  @SneakyThrows
  public <T extends JsonSerializable> T convertFromDocumentJson(String value) {
    if (EmptyPredicate.isEmpty(value)) {
      return null;
    }
    return convertFromDocument(Document.parse(value));
  }

  @SneakyThrows
  public Document convertToDocumentFromJson(String value) {
    if (EmptyPredicate.isEmpty(value)) {
      return null;
    }
    return Document.parse(value);
  }
}
