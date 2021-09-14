/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template;

import software.wings.beans.Variable;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReferencedTemplate {
  private TemplateReference templateReference;
  private Map<String, Variable> variableMapping;
}
