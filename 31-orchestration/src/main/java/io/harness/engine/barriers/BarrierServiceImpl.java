package io.harness.engine.barriers;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierNode;
import io.harness.engine.executions.barrier.BarrierNodeRepository;
import io.harness.exception.InvalidRequestException;

import java.util.List;

@OwnedBy(CDC)
public class BarrierServiceImpl implements BarrierService {
  @Inject private BarrierNodeRepository barrierNodeRepository;

  @Override
  public BarrierNode save(BarrierNode barrierNode) {
    return barrierNodeRepository.save(barrierNode);
  }

  @Override
  public BarrierNode get(String barrierUuid) {
    return barrierNodeRepository.findById(barrierUuid)
        .orElseThrow(() -> new InvalidRequestException("Barrier not found for id: " + barrierUuid));
  }

  @Override
  public List<BarrierNode> findByIdentifier(BarrierNode barrierNode) {
    return barrierNodeRepository.findByIdentifier(barrierNode.getIdentifier());
  }
}
