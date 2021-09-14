/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.yaml;

import static io.harness.annotations.dev.HarnessTeam.CV;

import io.harness.annotations.dev.OwnedBy;

import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Base class for all the yaml classes which are exposed as a .yaml file.
 * Note that not all yaml classes get exposed directly. Some of them are embedded within another yaml.
 * Such embedded classes extends BaseYamlWithType or BaseYaml.
 * @author rktummala on 10/17/17
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@OwnedBy(CV)
public abstract class BaseEntityYaml extends BaseYamlWithType {
  private String harnessApiVersion = "1.0";
  private Map<String, String> tags;

  public BaseEntityYaml(String type, String harnessApiVersion) {
    super(type);
    this.harnessApiVersion = harnessApiVersion;
  }
}
