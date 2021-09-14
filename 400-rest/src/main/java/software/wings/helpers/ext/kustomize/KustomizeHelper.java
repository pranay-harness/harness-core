/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.helpers.ext.kustomize;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.sm.ExecutionContext;

import com.google.inject.Singleton;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class KustomizeHelper {
  public void renderKustomizeConfig(@NotNull ExecutionContext context, @Nullable KustomizeConfig config) {
    if (config == null) {
      return;
    }
    if (isNotEmpty(config.getKustomizeDirPath())) {
      String kustomizeDirPath = config.getKustomizeDirPath().trim();
      kustomizeDirPath = render(
          context, kustomizeDirPath, String.format("Unable to render kustomize directory path : %s", kustomizeDirPath));
      config.setKustomizeDirPath(kustomizeDirPath);
    }

    if (isNotEmpty(config.getPluginRootDir())) {
      String pluginRootDir = config.getPluginRootDir().trim();
      pluginRootDir = render(
          context, pluginRootDir, String.format("Unable to render plugin root directory path : %s", pluginRootDir));
      config.setPluginRootDir(pluginRootDir);
    }
  }

  private String render(@NotNull ExecutionContext context, @NotNull String expression, String exceptionMessage) {
    String renderedValue = context.renderExpression(expression);
    if (renderedValue == null || "null".equalsIgnoreCase(renderedValue)) {
      throw new InvalidRequestException(exceptionMessage, WingsException.USER);
    }
    return renderedValue;
  }
}
