package io.harness.event.grpc;

import static io.harness.event.app.EventServiceApplication.EVENTS_DB;

import io.harness.annotation.StoreIn;
import io.harness.event.grpc.PublishedMessage.PublishedMessageKeys;
import io.harness.mongo.index.CdIndex;
import io.harness.mongo.index.FdTtlIndex;
import io.harness.mongo.index.Field;
import io.harness.persistence.AccountAccess;
import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import io.harness.persistence.UuidAware;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import java.time.OffsetDateTime;
import java.util.Date;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;

@StoreIn(EVENTS_DB)
@Data
@Entity(value = "publishedMessages", noClassnameStored = true)

@CdIndex(name = "accountId_type_CreatedAt_occurredAt",
    fields =
    {
      @Field(PublishedMessageKeys.accountId)
      , @Field(PublishedMessageKeys.type), @Field(PublishedMessageKeys.createdAt),
          @Field(PublishedMessageKeys.occurredAt)
    })
@FieldNameConstants(innerTypeName = "PublishedMessageKeys")
@Slf4j
public class PublishedMessage implements PersistentEntity, CreatedAtAware, UuidAware, AccountAccess {
  @Id private String uuid;
  private long createdAt;

  @EqualsAndHashCode.Exclude
  @Builder.Default
  @FdTtlIndex
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
      log.error("message type is {} createdAt {} occuredAt {} attr {}", type, createdAt, occurredAt, attributes);
    }
  }
}
