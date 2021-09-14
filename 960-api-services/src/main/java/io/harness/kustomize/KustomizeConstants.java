/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.kustomize;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;

import java.util.concurrent.TimeUnit;
import lombok.experimental.UtilityClass;

@OwnedBy(CDP)
@UtilityClass
public final class KustomizeConstants {
  static final String KUSTOMIZE_BINARY_PATH = "${KUSTOMIZE_BINARY_PATH}";
  static final String KUSTOMIZE_DIR_PATH = "${DIR_PATH}";
  static final String KUSTOMIZE_BUILD_COMMAND = KUSTOMIZE_BINARY_PATH + " build " + KUSTOMIZE_DIR_PATH;
  static final String XDG_CONFIG_HOME = "${XDG_CONFIG_HOME}";
  static final String KUSTOMIZE_BUILD_COMMAND_WITH_PLUGINS = "XDG_CONFIG_HOME=" + XDG_CONFIG_HOME + " "
      + KUSTOMIZE_BINARY_PATH + " build --enable_alpha_plugins " + KUSTOMIZE_DIR_PATH;
  static final long KUSTOMIZE_COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
}
