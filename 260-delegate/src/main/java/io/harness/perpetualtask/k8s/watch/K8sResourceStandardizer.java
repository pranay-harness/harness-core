package io.harness.perpetualtask.k8s.watch;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.kubernetes.client.custom.Quantity;
import java.math.BigDecimal;
import lombok.experimental.UtilityClass;

/**
 * Standardize units of cpu & memory resources.
 */
@UtilityClass
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
