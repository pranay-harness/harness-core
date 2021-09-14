/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.steps.shellscript;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.K8sInfraDelegateConfig;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG;
import io.harness.delegate.task.shell.ShellScriptTaskParametersNG.ShellScriptTaskParametersNGBuilder;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.shell.ScriptType;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

@OwnedBy(CDC)
public interface ShellScriptHelperService {
  // Handles ParameterField and String type Objects, else throws Exception
  Map<String, String> getEnvironmentVariables(Map<String, Object> inputVariables);
  List<String> getOutputVars(Map<String, Object> outputVariables);

  K8sInfraDelegateConfig getK8sInfraDelegateConfig(@Nonnull Ambiance ambiance, @Nonnull String shellScript);

  void prepareTaskParametersForExecutionTarget(@Nonnull Ambiance ambiance,
      @Nonnull ShellScriptStepParameters shellScriptStepParameters,
      @Nonnull ShellScriptTaskParametersNGBuilder taskParametersNGBuilder);

  String getShellScript(@Nonnull ShellScriptStepParameters stepParameters);

  String getWorkingDirectory(@Nonnull ShellScriptStepParameters stepParameters, @Nonnull ScriptType scriptType);

  ShellScriptTaskParametersNG buildShellScriptTaskParametersNG(
      @Nonnull Ambiance ambiance, @Nonnull ShellScriptStepParameters shellScriptStepParameters);

  ShellScriptOutcome prepareShellScriptOutcome(
      Map<String, String> sweepingOutputEnvVariables, Map<String, Object> outputVariables);
}
