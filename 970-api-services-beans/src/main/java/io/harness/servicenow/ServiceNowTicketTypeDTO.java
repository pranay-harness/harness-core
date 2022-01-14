package io.harness.servicenow;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.jackson.JsonNodeUtils;
import io.harness.servicenow.deserializer.ServiceNowTicketTypeDeserializer;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonDeserialize(using = ServiceNowTicketTypeDeserializer.class)
public class ServiceNowTicketTypeDTO {
  @NotNull String key;
  @NotNull String name;

  public ServiceNowTicketTypeDTO(JsonNode node) {
    this.key = JsonNodeUtils.mustGetString(node, "key");
    this.name = JsonNodeUtils.getString(node, "name");
    if (this.name == null) {
      this.name = this.key;
    }
  }
}
