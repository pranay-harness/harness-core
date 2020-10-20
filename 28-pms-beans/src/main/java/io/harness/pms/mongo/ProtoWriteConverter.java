package io.harness.pms.mongo;

import com.google.inject.Inject;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import lombok.SneakyThrows;
import org.springframework.core.convert.converter.Converter;

public abstract class ProtoWriteConverter<T extends Message> implements Converter<T, DBObject> {
  public ProtoWriteConverter() {}

  @SneakyThrows
  @Override
  public DBObject convert(T entity) {
    String entityJson = JsonFormat.printer().print(entity);
    return BasicDBObject.parse(entityJson);
  }
}
