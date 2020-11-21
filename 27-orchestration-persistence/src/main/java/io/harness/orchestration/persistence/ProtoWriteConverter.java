package io.harness.orchestration.persistence;

import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

public abstract class ProtoWriteConverter<T extends Message> implements Converter<T, Document> {
  @SneakyThrows
  @Override
  public Document convert(T entity) {
    String entityJson = JsonFormat.printer().print(entity);
    return Document.parse(entityJson);
  }
}
