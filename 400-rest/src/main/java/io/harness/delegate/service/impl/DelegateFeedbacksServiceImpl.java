/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.delegate.service.impl;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateFeedback;
import io.harness.delegate.beans.DelegateFeedbackDTO;
import io.harness.delegate.beans.DelegateFeedbackMapper;
import io.harness.delegate.service.intfc.DelegateFeedbacksService;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import lombok.RequiredArgsConstructor;

@OwnedBy(DEL)
@RequiredArgsConstructor(onConstructor = @__({ @Inject }))
public class DelegateFeedbacksServiceImpl implements DelegateFeedbacksService {
  private final HPersistence persistence;

  @Override
  public void persistFeedback(final String accountId, final DelegateFeedbackDTO delegateFeedbackDTO) {
    DelegateFeedback delegateFeedback = DelegateFeedbackMapper.map(delegateFeedbackDTO);
    persistence.save(delegateFeedback);
  }
}
