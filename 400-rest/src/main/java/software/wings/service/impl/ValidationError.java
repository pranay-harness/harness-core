/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import java.util.Collection;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class ValidationError {
  private String message;
  @Singular private Collection<String> restrictedFeatures;
}
