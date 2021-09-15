/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SampleBean;
import io.harness.git.model.ChangeType;
import io.harness.gitsync.persistance.GitAwarePersistence;
import io.harness.ng.core.utils.NGYamlUtils;

import com.google.inject.Inject;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@OwnedBy(DX)
public class TestCustomRepositoryImpl implements TestCustomRepository {
  private final GitAwarePersistence mongoTemplate;

  @Override
  public SampleBean save(SampleBean sampleBean) {
    return mongoTemplate.save(sampleBean, NGYamlUtils.getYamlString(sampleBean), ChangeType.ADD, SampleBean.class);
  }
}
