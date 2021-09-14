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
 *
 */
@Data
@Builder
public class AccountSalesContactsInfo {
  List<String> salesContacts;
}
