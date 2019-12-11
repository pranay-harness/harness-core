package io.harness.perpetualtask.k8s.watch;

import io.kubernetes.client.custom.Quantity;
import lombok.experimental.UtilityClass;

import java.math.BigDecimal;

/**
 * Standardize units of cpu & memory resources.
 */
@UtilityClass
public class K8sResourceStandardizer {
  private static final BigDecimal SCALE_TO_NANO = BigDecimal.valueOf(1_000_000_000);

  // Standardize cpu as nanocores (as usage is given in nanocore precision)
  public long getCpuNano(String cpu) {
    return Quantity.fromString(cpu).getNumber().multiply(SCALE_TO_NANO).longValueExact();
  }

  // Standardize memory as bytes
  public long getMemoryByte(String mem) {
    return Quantity.fromString(mem).getNumber().longValueExact();
  }
}
