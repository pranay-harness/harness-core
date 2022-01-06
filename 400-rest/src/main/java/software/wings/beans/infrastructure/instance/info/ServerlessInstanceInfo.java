/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.infrastructure.instance.info;

import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;

import java.util.Map;
import lombok.Data;
import lombok.experimental.FieldNameConstants;

@Data
@FieldNameConstants(innerTypeName = "ServerlessInstanceInfoKeys")
public abstract class ServerlessInstanceInfo {
  private Map<InvocationCountKey, InvocationCount> invocationCountMap;

  public ServerlessInstanceInfo(Map<InvocationCountKey, InvocationCount> invocationCountMap) {
    this.invocationCountMap = invocationCountMap;
  }
}
