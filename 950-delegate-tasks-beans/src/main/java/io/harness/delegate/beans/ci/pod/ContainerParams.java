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

@Data
@AllArgsConstructor
public abstract class ContainerParams implements NestedAnnotationResolver {
  private String name;
  @Expression(ALLOW_SECRETS) private ImageDetailsWithConnector imageDetailsWithConnector;
  private List<String> commands;
  @Expression(ALLOW_SECRETS) private List<String> args;
  @Expression(ALLOW_SECRETS) private String workingDir;
  private List<Integer> ports;
  @Expression(ALLOW_SECRETS) private Map<String, String> envVars;
  @Expression(ALLOW_SECRETS) private Map<String, String> envVarsWithSecretRef;
  private Map<String, SecretVarParams> secretEnvVars;
  private Map<String, SecretVolumeParams> secretVolumes;
  @Expression(ALLOW_SECRETS) private String imageSecret;
  @Expression(ALLOW_SECRETS) private Map<String, String> volumeToMountPath;
  private ContainerResourceParams containerResourceParams;
  private ContainerSecrets containerSecrets;
  private Integer runAsUser;
  private boolean privileged;
  private String imagePullPolicy;

  public abstract ContainerParams.Type getType();

  public enum Type {
    K8, // Generic K8 container configuration
    K8_GIT_CLONE, // K8 container configuration to clone a git repository
  }
}
