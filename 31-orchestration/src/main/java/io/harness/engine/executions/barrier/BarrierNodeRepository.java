package io.harness.engine.executions.barrier;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.barriers.BarrierNode;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

@OwnedBy(CDC)
@HarnessRepo
public interface BarrierNodeRepository extends CrudRepository<BarrierNode, String> {
  List<BarrierNode> findByIdentifier(String identifier);
}
