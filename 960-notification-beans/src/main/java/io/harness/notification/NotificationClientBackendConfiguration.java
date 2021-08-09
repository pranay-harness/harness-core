package io.harness.notification;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "type", visible = true)
@JsonSubTypes(value = { @JsonSubTypes.Type(value = MongoBackendConfiguration.class, name = "MONGO") })
@Data
public abstract class NotificationClientBackendConfiguration {
  @JsonProperty("type") String type;
}
