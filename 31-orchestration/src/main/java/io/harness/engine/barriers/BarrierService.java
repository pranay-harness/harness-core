package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierNode;

import java.util.List;

@OwnedBy(CDC)
public interface BarrierService {
  BarrierNode save(BarrierNode barrierNode);
  BarrierNode get(String barrierUuid);
  List<BarrierNode> findByIdentifier(BarrierNode barrierNode);
}
