package io.harness.repositories.ng.webhook.spring;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotation.HarnessRepo;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.webhook.entities.WebhookEvent;

import org.springframework.data.repository.PagingAndSortingRepository;

@HarnessRepo
@OwnedBy(PIPELINE)
public interface WebhookEventRepository extends PagingAndSortingRepository<WebhookEvent, String> {}
