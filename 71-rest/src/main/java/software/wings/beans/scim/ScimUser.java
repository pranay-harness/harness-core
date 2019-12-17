package software.wings.beans.scim;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScimUser extends ScimBaseResource {
  private Set<String> schemas = new HashSet<>(Arrays.asList("urn:ietf:params:scim:schemas:core:2.0:User"));
  private String userName;
  private String displayName;
  private Boolean active;
  private JsonNode emails;
  private JsonNode roles;
  private JsonNode name;
  private JsonNode groups;
  private JsonNode password;
}
