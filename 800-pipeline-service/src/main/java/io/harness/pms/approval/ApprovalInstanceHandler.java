/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.pms.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofSeconds;

import io.harness.PipelineServiceIteratorsConfig;
import io.harness.annotations.dev.OwnedBy;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.IteratorConfig;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceRequiredProvider;
import io.harness.steps.approval.step.beans.ApprovalStatus;
import io.harness.steps.approval.step.beans.ApprovalType;
import io.harness.steps.approval.step.entities.ApprovalInstance;
import io.harness.steps.approval.step.entities.ApprovalInstance.ApprovalInstanceKeys;
import io.harness.steps.approval.step.jira.JiraApprovalHelperService;
import io.harness.steps.approval.step.jira.entities.JiraApprovalInstance;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;

@OwnedBy(CDC)
@Slf4j
public class ApprovalInstanceHandler implements MongoPersistenceIterator.Handler<ApprovalInstance> {
  private static final IteratorConfig DEFAULT_ITERATOR_CONFIG =
      IteratorConfig.builder().enabled(true).targetIntervalInSeconds(60).build();

  private final JiraApprovalHelperService jiraApprovalHelperService;
  private final MongoTemplate mongoTemplate;
  private final PersistenceIteratorFactory persistenceIteratorFactory;
  private final PipelineServiceIteratorsConfig iteratorsConfig;

  @Inject
  public ApprovalInstanceHandler(JiraApprovalHelperService jiraApprovalHelperService, MongoTemplate mongoTemplate,
      PersistenceIteratorFactory persistenceIteratorFactory, PipelineServiceIteratorsConfig iteratorsConfig) {
    this.jiraApprovalHelperService = jiraApprovalHelperService;
    this.mongoTemplate = mongoTemplate;
    this.persistenceIteratorFactory = persistenceIteratorFactory;
    this.iteratorsConfig = iteratorsConfig;
  }

  public void registerIterators() {
    IteratorConfig iteratorConfig = iteratorsConfig.getApprovalInstanceIteratorConfig();
    if (iteratorConfig == null) {
      iteratorConfig = DEFAULT_ITERATOR_CONFIG;
    }
    if (!iteratorConfig.isEnabled()) {
      return;
    }

    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("ApprovalInstanceHandler")
            .poolSize(2)
            .interval(ofSeconds(iteratorConfig.getTargetIntervalInSeconds()))
            .build(),
        ApprovalInstanceHandler.class,
        MongoPersistenceIterator.<ApprovalInstance, SpringFilterExpander>builder()
            .clazz(ApprovalInstance.class)
            .fieldName(ApprovalInstanceKeys.nextIteration)
            .targetInterval(ofSeconds(iteratorConfig.getTargetIntervalInSeconds()))
            .acceptableNoAlertDelay(ofSeconds(iteratorConfig.getTargetIntervalInSeconds() * 2))
            .handler(this)
            .filterExpander(query
                -> query.addCriteria(Criteria.where(ApprovalInstanceKeys.status)
                                         .is(ApprovalStatus.WAITING)
                                         .and(ApprovalInstanceKeys.type)
                                         .in(ApprovalType.JIRA_APPROVAL)))
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceRequiredProvider<>(mongoTemplate))
            .redistribute(true));
  }

  @Override
  public void handle(ApprovalInstance entity) {
    switch (entity.getType()) {
      case JIRA_APPROVAL:
        JiraApprovalInstance jiraApprovalInstance = (JiraApprovalInstance) entity;
        jiraApprovalHelperService.handlePollingEvent(jiraApprovalInstance);
        break;
      default:
        log.warn("ApprovalInstance without registered handler encountered. Id: {}", entity.getId());
    }
  }
}
