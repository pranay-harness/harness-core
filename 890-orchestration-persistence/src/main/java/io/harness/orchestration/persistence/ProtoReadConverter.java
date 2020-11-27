package io.harness.orchestration.persistence;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;
import lombok.SneakyThrows;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;

@SuppressWarnings("unchecked")
public abstract class ProtoReadConverter<T extends Message> implements Converter<Document, T> {
  private final Class<T> entityClass;

  @Inject
  public ProtoReadConverter(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @SneakyThrows
  @Override
  public T convert(Document dbObject) {
    Builder builder = (Builder) entityClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(dbObject.toJson(), builder);
    return (T) builder.build();
  }
}
