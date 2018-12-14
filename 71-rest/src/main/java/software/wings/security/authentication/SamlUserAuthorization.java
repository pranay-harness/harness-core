package software.wings.security.authentication;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SamlUserAuthorization {
  private String email;
  private List<String> userGroups;
}
