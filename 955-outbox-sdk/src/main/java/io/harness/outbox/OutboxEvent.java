package io.harness.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Getter
@Builder
@FieldNameConstants(innerTypeName = "OutboxEventKeys")
@Entity(value = "outboxEvents", noClassnameStored = true)
@Document("outboxEvents")
public class OutboxEvent {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull String type;
  @NotNull JsonNode data;

  Map<String, String> additionalData;

  @CreatedDate Long createdAt;
}