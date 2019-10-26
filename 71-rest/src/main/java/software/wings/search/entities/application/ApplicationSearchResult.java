package software.wings.search.entities.application;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.search.entities.related.audit.RelatedAuditView;
import software.wings.search.framework.EntityInfo;
import software.wings.search.framework.SearchEntityUtils;
import software.wings.search.framework.SearchResult;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * The Response View of Applications in which the
 * Search Hits from Elk will  be wrapped
 *
 * @author ujjawal
 */

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
  private static final long startTimestampToRetainFrom =
      SearchEntityUtils.getTimestampNdaysBackInMillis(DAYS_TO_RETAIN);

  private void setAudits(ApplicationView applicationView) {
    if (!applicationView.getAudits().isEmpty()) {
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
