package io.harness.scim;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScimBaseResource {
  private String id;
  private String externalId;
  private JsonNode meta;
}
