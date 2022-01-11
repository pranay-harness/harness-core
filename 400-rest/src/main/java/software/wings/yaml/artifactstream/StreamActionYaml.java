/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.yaml.artifactstream;

import io.harness.yaml.BaseYaml;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
public class StreamActionYaml extends BaseYaml {
  public String workflowType;
  public String workflowName;
  public String envName;

  public static final class Builder {
    public String workflowType;
    public String workflowName;
    public String envName;

    private Builder() {}

    public static Builder aStreamActionYaml() {
      return new Builder();
    }

    public Builder withWorkflowType(String workflowType) {
      this.workflowType = workflowType;
      return this;
    }

    public Builder withWorkflowName(String workflowName) {
      this.workflowName = workflowName;
      return this;
    }

    public Builder withEnvName(String envName) {
      this.envName = envName;
      return this;
    }

    public Builder but() {
      return aStreamActionYaml().withWorkflowType(workflowType).withWorkflowName(workflowName).withEnvName(envName);
    }

    public StreamActionYaml build() {
      StreamActionYaml streamActionYaml = new StreamActionYaml();
      streamActionYaml.setWorkflowType(workflowType);
      streamActionYaml.setWorkflowName(workflowName);
      streamActionYaml.setEnvName(envName);
      return streamActionYaml;
    }
  }
}
