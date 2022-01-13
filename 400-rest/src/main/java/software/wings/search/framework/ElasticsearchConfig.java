/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.framework;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Elasticsearch Configuration for search
 *
 * @author utkarsh
 */
@OwnedBy(PL)
@Value
@Builder
public class ElasticsearchConfig {
  @JsonProperty(defaultValue = "http://localhost:9200")
  @Builder.Default
  @NotEmpty
  private String uri = "http://localhost:9200";
  @JsonProperty(defaultValue = "_default") @Builder.Default @NotEmpty private String indexSuffix = "_default";
  @JsonProperty(defaultValue = "none") @Builder.Default @NotEmpty private String mongoTagKey = "none";
  @JsonProperty(defaultValue = "none") @Builder.Default @NotEmpty private String mongoTagValue = "none";

  private byte[] encryptedUri;
}
