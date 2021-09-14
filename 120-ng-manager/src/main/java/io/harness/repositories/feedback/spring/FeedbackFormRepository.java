/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
