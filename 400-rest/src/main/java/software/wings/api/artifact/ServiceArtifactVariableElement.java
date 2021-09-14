/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.api.artifact;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.context.ContextElementType;
import io.harness.expression.ExpressionEvaluator;

import software.wings.beans.EntityType;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ServiceArtifactVariableElement implements ContextElement {
  private String uuid;
  private String name;
  private EntityType entityType;
  private String entityId;
  private String serviceId;
  private String artifactVariableName;

  @Override
  public ContextElementType getElementType() {
    return ContextElementType.ARTIFACT_VARIABLE;
  }

  public String getArtifactVariableName() {
    return isBlank(artifactVariableName) ? ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME : artifactVariableName;
  }

  @Override
  public Map<String, Object> paramMap(ExecutionContext context) {
    return null;
  }

  @Override
  public ContextElement cloneMin() {
    return this;
  }
}
