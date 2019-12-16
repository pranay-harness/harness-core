package io.harness.event.grpc;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.harness.annotation.StoreIn;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import io.harness.exception.WingsException;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PostLoad;

import java.util.Map;

@StoreIn(EVENTS_DB)
@Data
@Entity(value = "publishedMessages", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "type_CreatedAt"), fields = {
    @Field(PublishedMessageKeys.type), @Field(PublishedMessageKeys.createdAt)
  })
})
@FieldNameConstants(innerTypeName = "PublishedMessageKeys")
public class PublishedMessage implements PersistentEntity, CreatedAtAware, UuidAware {
  @Id private String uuid;
  private long createdAt;

  private final long occurredAt;
  private final String accountId;
  private final String type;
  private final byte[] data;
  private final Map<String, String> attributes;

  @Setter(AccessLevel.NONE) private transient Message message;

  @Builder
  private PublishedMessage(
      String accountId, String type, byte[] data, Map<String, String> attributes, long occurredAt) {
    this.accountId = accountId;
    this.type = type;
    this.data = data;
    this.attributes = attributes;
    this.occurredAt = occurredAt;
  }

  public Message getMessage() {
    if (message == null) {
      postLoad();
    }
    return message;
  }

  @PostLoad
  private void postLoad() {
    try {
      Any any = Any.parseFrom(data);
      @SuppressWarnings("unchecked") Class<? extends Message> clazz = (Class<? extends Message>) Class.forName(type);
      this.message = any.unpack(clazz);
    } catch (ClassNotFoundException | InvalidProtocolBufferException e) {
      throw new WingsException("Unable to parse message for type: " + type, e);
    }
  }
}
