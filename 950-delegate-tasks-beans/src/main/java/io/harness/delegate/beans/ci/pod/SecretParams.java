/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.beans.ci.pod;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretParams {
  public enum Type { FILE, TEXT }
  private String value;
  private String secretKey;
  private Type type;
}
