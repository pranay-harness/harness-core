package software.wings.beans.sso;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.experimental.FieldDefaults;
import org.hibernate.validator.constraints.NotBlank;

import java.util.Collection;
import java.util.Collections;

@OwnedBy(PL)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LdapGroupResponse {
  @NotBlank String dn;
  @NotBlank String name;
  String description;
  int totalMembers;
  boolean selectable;
  String message;
  @Default Collection<LdapUserResponse> users = Collections.emptyList();
}
