package io.harness.scim;

import java.net.URI;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Member {
  private String value;
  private URI ref;
  private String display;
}
