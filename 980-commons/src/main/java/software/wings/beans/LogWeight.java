/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.beans;

public enum LogWeight {
  Normal(0),
  Bold(1);

  final int value;

  LogWeight(int value) {
    this.value = value;
  }
}
