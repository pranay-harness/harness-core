/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.sm;

/**
 * Apis to be implemented by ContextElements needed by verification.
 */
public interface VerificationElement {
  boolean isNewInstance();
}
