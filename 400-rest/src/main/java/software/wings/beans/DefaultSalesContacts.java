/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import java.util.List;
import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 01/11/19
 */

@Data
@Builder
public class DefaultSalesContacts {
  private boolean enabled;
  private List<AccountTypeDefault> accountTypeDefaults;

  @Data
  @Builder
  public static class AccountTypeDefault {
    private String accountType;
    private String emailIds;
  }
}
