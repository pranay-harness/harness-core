/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SweepingOutputInstance;
import io.harness.expression.LateBindingValue;
import io.harness.serializer.KryoSerializer;

import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;

import lombok.Builder;

@OwnedBy(CDC)
@Builder
public class SweepingOutputValue implements LateBindingValue {
  private SweepingOutputService sweepingOutputService;
  private KryoSerializer kryoSerializer;
  private SweepingOutputInquiry sweepingOutputInquiry;

  @Override
  public Object bind() {
    SweepingOutputInstance sweepingOutputInstance = sweepingOutputService.find(sweepingOutputInquiry);
    if (sweepingOutputInstance == null) {
      return null;
    }

    if (sweepingOutputInstance.getValue() != null) {
      return sweepingOutputInstance.getValue();
    }

    return kryoSerializer.asInflatedObject(sweepingOutputInstance.getOutput());
  }
}
