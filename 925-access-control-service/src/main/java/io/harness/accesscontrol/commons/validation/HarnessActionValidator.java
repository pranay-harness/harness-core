/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.accesscontrol.commons.validation;

import io.harness.accesscontrol.common.validation.ValidationResult;

public interface HarnessActionValidator<T> {
  ValidationResult canDelete(T object);
  ValidationResult canCreate(T object);
  ValidationResult canUpdate(T object);
}
