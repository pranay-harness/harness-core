/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.customdeployment;

import software.wings.beans.Variable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomDeploymentTypeDTO {
  /*
  Id of the custom deployment template
   */
  private String uuid;
  private String name;
  private List<Variable> infraVariables;
}
