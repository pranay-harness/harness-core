/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.beans.ci.pod;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;

import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public abstract class PodParams<T extends ContainerParams> implements NestedAnnotationResolver {
  @NonNull private String name;
  @Expression(ALLOW_SECRETS) @NonNull private String namespace;
  @Expression(ALLOW_SECRETS) private Map<String, String> annotations;
  @Expression(ALLOW_SECRETS) private Map<String, String> labels;
  @Expression(ALLOW_SECRETS) private List<T> containerParamsList;
  private List<T> initContainerParamsList;
  private List<PVCParams> pvcParamList;
  private List<HostAliasParams> hostAliasParamsList;
  private Integer runAsUser;
  private String serviceAccountName;

  public abstract PodParams.Type getType();

  public enum Type { K8 }
}
