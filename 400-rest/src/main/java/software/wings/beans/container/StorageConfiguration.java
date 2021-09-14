/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.container;

import io.harness.yaml.BaseYaml;

import com.github.reinert.jjschema.Attributes;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@Builder
public class StorageConfiguration {
  @Attributes(title = "Host Source Path") private String hostSourcePath;
  @Attributes(title = "Container Path") private String containerPath;
  @Attributes(title = "Options") private boolean readonly;

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String hostSourcePath;
    private String containerPath;
    private boolean readonly;

    @Builder
    public Yaml(String hostSourcePath, String containerPath, boolean readonly) {
      this.hostSourcePath = hostSourcePath;
      this.containerPath = containerPath;
      this.readonly = readonly;
    }
  }
}
