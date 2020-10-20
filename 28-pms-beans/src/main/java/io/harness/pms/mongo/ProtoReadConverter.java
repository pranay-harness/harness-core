package io.harness.pms.mongo;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.Message.Builder;
import com.google.protobuf.util.JsonFormat;

import com.mongodb.DBObject;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;

public abstract class ProtoReadConverter<T extends Message> implements Converter<DBObject, T> {
  private final Class<T> entityClass;

  @Inject
  public ProtoReadConverter(Class<T> entityClass) {
    this.entityClass = entityClass;
  }

  @SneakyThrows
  @Override
  public T convert(DBObject dbObject) {
    Builder builder = null;
    builder = (Builder) entityClass.getMethod("newBuilder").invoke(null);
    JsonFormat.parser().ignoringUnknownFields().merge(dbObject.toString(), builder);
    return (T) builder.build();
  }
}
