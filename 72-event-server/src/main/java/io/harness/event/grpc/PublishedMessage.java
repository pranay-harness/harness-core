package io.harness.event.grpc;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;

import io.harness.annotation.StoreIn;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import io.harness.exception.DataFormatException;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import org.mongodb.morphia.annotations.PostLoad;

import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;

@StoreIn(EVENTS_DB)
@Data
@Entity(value = "publishedMessages", noClassnameStored = true)
@Indexes({
  @Index(options = @IndexOptions(name = "accountId_type_CreatedAt_occurredAt", background = true), fields = {
    @Field(PublishedMessageKeys.accountId)
    , @Field(PublishedMessageKeys.type), @Field(PublishedMessageKeys.createdAt), @Field(PublishedMessageKeys.occurredAt)
  })
})
@FieldNameConstants(innerTypeName = "PublishedMessageKeys")
@Slf4j
public class PublishedMessage implements PersistentEntity, CreatedAtAware, UuidAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;

  @EqualsAndHashCode.Exclude
  @Builder.Default
  @Indexed(options = @IndexOptions(expireAfterSeconds = 0))
  private Date validUntil = Date.from(OffsetDateTime.now().plusDays(14).toInstant());

  private final long occurredAt;
  private final String accountId;
  private final String type;
  private final byte[] data;
  private final String category;
  private final Map<String, String> attributes;

  @Setter(AccessLevel.NONE) private transient Message message;

  @Builder(toBuilder = true)
  private PublishedMessage(String uuid, String accountId, String type, byte[] data, Message message, String category,
      Map<String, String> attributes, long occurredAt) {
    this.uuid = uuid;
    this.accountId = accountId;
    this.type = type;
    this.data = data;
    this.message = message;
    this.category = category;
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
      logger.error("message type is {} createdAt {} occuredAt {} attr {}", type, createdAt, occurredAt, attributes);
      throw new DataFormatException("Unable to parse message for type: " + type, e);
    }
  }
}
