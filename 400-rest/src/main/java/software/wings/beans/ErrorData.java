/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import static java.util.Collections.EMPTY_LIST;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ErrorData {
  private Exception exception;
  @Builder.Default private String email = "";
  @Builder.Default private List<BugsnagTab> tabs = EMPTY_LIST;
}
