/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.yaml.BaseYamlWithType;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@OwnedBy(CDC)
@Data
public class WebHookEventConfig {
  @Attributes(required = true, title = "URL") private String url;
  @Getter @Setter private List<KeyValuePair> headers;
  @Getter @Setter @Attributes(title = "Use Delegate Proxy") private boolean useProxy;
  @Getter @Setter @Attributes(title = "Tags") private List<String> tags;
  @SchemaIgnore private int socketTimeoutMillis = 30000;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = false)
  public static final class Yaml extends BaseYamlWithType {
    private String url;
    private List<KeyValuePair> headers;
    @lombok.Builder
    public Yaml(String url, List<KeyValuePair> headers) {
      this.url = url;
      this.headers = headers;
    }
  }
}
