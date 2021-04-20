package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;

@OwnedBy(DX)
public interface TestCustomRepository {
  SampleBean save(SampleBean sampleBean);
}
