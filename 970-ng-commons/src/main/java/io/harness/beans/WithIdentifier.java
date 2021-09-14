/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans;

import javax.validation.constraints.NotNull;

/**
 *  Any class that has identifier property should implement this interface
 */
public interface WithIdentifier {
  /**
   * Non-changeable identifier of the pipeline, can not contain spaces or special chars. REQUIRED
   * @return identifier
   */
  @NotNull String getIdentifier();
}
