/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.serializer.json;

import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.serializer.json.serializers.ProtoJsonSerializer;

public class ExecutableResponseSerializer extends ProtoJsonSerializer<ExecutableResponse> {
  public ExecutableResponseSerializer(Class<ExecutableResponse> t) {
    super(t);
  }

  public ExecutableResponseSerializer() {
    this(null);
  }
}
