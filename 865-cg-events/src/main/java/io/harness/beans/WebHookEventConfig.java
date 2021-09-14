/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@Data
public class WebHookEventConfig {
  @Attributes(required = true, title = "URL") private String url;
  @Getter @Setter private List<KeyValuePair> headers;
  @Getter @Setter @Attributes(title = "Use Delegate Proxy") private boolean useProxy;
  @Getter @Setter @Attributes(title = "Tags") private List<String> tags;
  @SchemaIgnore private int socketTimeoutMillis = 30000;
}
