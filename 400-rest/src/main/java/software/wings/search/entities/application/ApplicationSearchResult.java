/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.search.entities.application;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;

import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * The Response View of Applications in which the
 * Search Hits from Elk will  be wrapped
 *
 * @author ujjawal
 */

@OwnedBy(PL)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ApplicationSearchResult extends SearchResult {
  private Set<EntityInfo> services;
  private Set<EntityInfo> environments;
  private Set<EntityInfo> workflows;
  private Set<EntityInfo> pipelines;
  private List<RelatedAuditView> audits;
  private Integer auditsCount = 0;
  private static final int MAX_ENTRIES = 3;
  private static final int DAYS_TO_RETAIN = 7;

  public void setAudits(List<RelatedAuditView> audits) {
    if (EmptyPredicate.isNotEmpty(audits)) {
      this.audits = audits;
      this.auditsCount = audits.size();
    } else {
      this.audits = new ArrayList<>();
      this.auditsCount = 0;
    }
  }

  private void setAudits(ApplicationView applicationView) {
    long startTimestampToRetainFrom = SearchEntityUtils.getTimestampNdaysBackInMillis(DAYS_TO_RETAIN);
    if (EmptyPredicate.isNotEmpty(applicationView.getAudits())) {
      this.auditsCount =
          SearchEntityUtils.truncateList(applicationView.getAuditTimestamps(), startTimestampToRetainFrom).size();
      removeStaleAuditEntries(applicationView);
    }
  }

  private void removeStaleAuditEntries(ApplicationView applicationView) {
    if (this.auditsCount >= MAX_ENTRIES) {
      this.audits = applicationView.getAudits();
    } else {
      int length = applicationView.getAudits().size();
      this.audits = applicationView.getAudits().subList(length - this.auditsCount, length);
    }
    Collections.reverse(this.audits);
  }

  public ApplicationSearchResult(ApplicationView applicationView, boolean includeAudits, float searchScore) {
    super(applicationView.getId(), applicationView.getName(), applicationView.getDescription(),
        applicationView.getAccountId(), applicationView.getCreatedAt(), applicationView.getLastUpdatedAt(),
        applicationView.getType(), applicationView.getCreatedBy(), applicationView.getLastUpdatedBy(), searchScore);
    this.services = applicationView.getServices();
    this.environments = applicationView.getEnvironments();
    this.workflows = applicationView.getWorkflows();
    this.pipelines = applicationView.getPipelines();
    if (includeAudits) {
      setAudits(applicationView);
    }
  }
}
