package io.harness.repositories;

import io.harness.beans.SampleBean;
import io.harness.gitsync.persistance.GitAwarePersistence;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
public class TestCustomRepositoryImpl implements TestCustomRepository {
  private final GitAwarePersistence mongoTemplate;

  @Override
  public SampleBean save(SampleBean sampleBean) {
    return mongoTemplate.save(sampleBean, sampleBean.toString());
  }
}
