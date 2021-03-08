package io.harness.repositories;

import io.harness.beans.SampleBean;

public interface TestCustomRepository {
  SampleBean save(SampleBean sampleBean);
}
