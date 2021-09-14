/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * @author rktummala on 06/19/19
 */

@Data
@Builder
public class UrlInfo {
  private String title;
  private String url;
}
