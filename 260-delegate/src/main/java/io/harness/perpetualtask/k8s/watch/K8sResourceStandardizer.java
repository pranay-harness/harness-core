/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.perpetualtask.k8s.watch;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import io.kubernetes.client.custom.Quantity;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

/**
 * Standardize units of cpu & memory resources.
 */
@OwnedBy(HarnessTeam.CE)
@UtilityClass
@TargetModule(HarnessModule._420_DELEGATE_AGENT)
public class K8sResourceStandardizer {
  private static final BigDecimal SCALE_TO_NANO = BigDecimal.valueOf(1_000_000_000);

  // Standardize cpu as nanocores (as usage is given in nanocore precision)
  public long getCpuNano(String cpu) {
    return getCpuCores(cpu).multiply(SCALE_TO_NANO).longValue();
  }

  public BigDecimal getCpuCores(String cpu) {
    if (isEmpty(cpu)) {
      return BigDecimal.ZERO;
    }
    return Quantity.fromString(cpu).getNumber();
  }

  public long getCpuNano(Quantity cpu) {
    if (cpu == null || cpu.getNumber() == null) {
      return 0L;
    }
    return cpu.getNumber().multiply(SCALE_TO_NANO).longValue();
  }

  // Standardize memory as bytes
  public long getMemoryByte(String mem) {
    if (isEmpty(mem)) {
      return 0L;
    }
    return Quantity.fromString(mem).getNumber().longValue();
  }

  public long getMemoryByte(Quantity mem) {
    if (mem == null || mem.getNumber() == null) {
      return 0L;
    }
    return mem.getNumber().longValue();
  }
}
