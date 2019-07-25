package io.harness.event.grpc;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.harness.exception.WingsException;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;

import java.util.Map;

@Data
@Builder
@Entity(value = "publishedMessages", noClassnameStored = true)
public class PublishedMessage implements PersistentEntity, CreatedAtAware {
  @Id private final String messageId;
  private final String accountId;
  private final String type;
  private final byte[] data;
  private final Map<String, String> attributes;

  private transient Message message;
  private long createdAt;

  @PostLoad
  void postLoad() {
    try {
      Any any = Any.parseFrom(data);
      @SuppressWarnings("unchecked") Class<? extends Message> clazz = (Class<? extends Message>) Class.forName(type);
      this.message = any.unpack(clazz);
    } catch (ClassNotFoundException | InvalidProtocolBufferException e) {
      throw new WingsException("Unable to parse message for type: " + type, e);
    }
  }
}
