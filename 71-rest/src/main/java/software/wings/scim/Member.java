package software.wings.scim;

import lombok.Getter;
import lombok.Setter;

import java.net.URI;

@Getter
@Setter
public class Member {
  private String value;
  private URI ref;
  private String display;
}
