package io.harness.cvng.activity.entities;

import static io.harness.cvng.beans.activity.ActivityType.PAGER_DUTY;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@FieldNameConstants(innerTypeName = "PagerDutyActivityKeys")
@Data
@SuperBuilder
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PagerDutyActivity extends Activity {
  private String eventId;
  private String pagerDutyUrl;

  @Override
  public ActivityType getType() {
    return PAGER_DUTY;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    throw new UnsupportedOperationException("Pagerduty Events can be transformed only from ChangeEventDTO");
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstanceBuilder) {
    throw new UnsupportedOperationException("Pagerduty Events shouldn't have verification jobs");
  }

  @Override
  public void validateActivityParams() {}

  @Override
  public boolean deduplicateEvents() {
    return false;
  }

  public static class PagerDutyActivityUpdatableEntity
      extends ActivityUpdatableEntity<PagerDutyActivity, PagerDutyActivity> {
    @Override
    public Class getEntityClass() {
      return PagerDutyActivity.class;
    }

    public Query<PagerDutyActivity> populateKeyQuery(Query<PagerDutyActivity> query, PagerDutyActivity activity) {
      return super.populateKeyQuery(query, activity)
          .filter(PagerDutyActivityKeys.eventId, activity.getEventId())
          .filter(PagerDutyActivityKeys.pagerDutyUrl, activity.getPagerDutyUrl());
    }

    @Override
    public void setUpdateOperations(UpdateOperations<PagerDutyActivity> updateOperations, PagerDutyActivity activity) {
      setCommonUpdateOperations(updateOperations, activity);
      updateOperations.set(PagerDutyActivityKeys.eventId, activity.getEventId())
          .set(PagerDutyActivityKeys.pagerDutyUrl, activity.getPagerDutyUrl());
    }
  }
}
