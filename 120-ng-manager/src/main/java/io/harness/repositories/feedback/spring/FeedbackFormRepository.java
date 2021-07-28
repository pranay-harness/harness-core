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
