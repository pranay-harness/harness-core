package io.harness.scim;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(Include.NON_NULL)
public class ScimGroup extends ScimBaseResource {
  private Set<String> schemas = new HashSet<>(Arrays.asList("urn:ietf:params:scim:schemas:core:2.0:Group"));
  private String displayName;
  private List<Member> members;
  private String harnessScopes;
}
