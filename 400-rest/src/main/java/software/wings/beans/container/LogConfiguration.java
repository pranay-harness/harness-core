/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.container;

import static io.harness.annotations.dev.HarnessModule._870_CG_YAML;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import software.wings.beans.NameValuePair;

import com.github.reinert.jjschema.Attributes;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(PL)
@Data
@Builder
@TargetModule(_870_CG_YAML)
public class LogConfiguration {
  @Attributes(title = "Log Driver") private String logDriver;
  @Attributes(title = "Options") private List<LogOption> options;

  public static class LogOption {
    private String key;
    private String value;

    public String getKey() {
      return key;
    }

    public void setKey(String key) {
      this.key = key;
    }

    public String getValue() {
      return value;
    }

    public void setValue(String value) {
      this.value = value;
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String logDriver;
    private List<NameValuePair.Yaml> options;

    @Builder
    public Yaml(String logDriver, List<NameValuePair.Yaml> options) {
      this.logDriver = logDriver;
      this.options = options;
    }
  }
}
