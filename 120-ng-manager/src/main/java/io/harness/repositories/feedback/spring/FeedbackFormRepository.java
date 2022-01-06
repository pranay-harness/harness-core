/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.repositories.feedback.spring;

import static io.harness.annotations.dev.HarnessTeam.GTM;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.feedback.entities.FeedbackForm;

import org.springframework.data.repository.CrudRepository;
import org.springframework.transaction.annotation.Transactional;

@HarnessRepo
@Transactional
@OwnedBy(GTM)
public interface FeedbackFormRepository extends CrudRepository<FeedbackForm, String> {}
