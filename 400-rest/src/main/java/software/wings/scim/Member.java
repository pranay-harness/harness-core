/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.scim;

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
