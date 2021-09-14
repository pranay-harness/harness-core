/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AccountJoinRequest {
  private String name;
  private String email;
  private String companyName;
  private String note;
  private String accountAdminEmail;
}
