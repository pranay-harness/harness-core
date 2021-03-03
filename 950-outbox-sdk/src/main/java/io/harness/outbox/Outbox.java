package io.harness.outbox;

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
@FieldNameConstants(innerTypeName = "OutboxKeys")
@Entity(value = "outbox")
@Document("outbox")
public class Outbox {
  @Id @org.mongodb.morphia.annotations.Id String id;

  @NotNull String type;
  @NotNull Object object;

  Map<String, String> additionalData;

  @CreatedDate Long createdAt;
}