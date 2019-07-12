package io.harness.event.grpc;

import com.google.protobuf.Message;

import io.harness.persistence.CreatedAtAware;
import io.harness.persistence.PersistentEntity;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;
import org.mongodb.morphia.annotations.PostLoad;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
  private void postLoad() {
    try {
      Class<?> clazz = Class.forName(type);
      Method parseFrom = clazz.getMethod("parseFrom", byte[].class);
      @SuppressWarnings("PrimitiveArrayArgumentToVarargsMethod")
      Message parsedMessage = (Message) parseFrom.invoke(null, data);
      this.message = parsedMessage;
    } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }
}
