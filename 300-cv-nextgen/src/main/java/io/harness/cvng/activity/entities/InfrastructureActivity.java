package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@SuperBuilder
@JsonTypeName("INFRASTRUCTURE")
public class InfrastructureActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.INFRASTRUCTURE;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    addCommonFields(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstance) {
    verificationJobInstance.startTime(getActivityStartTime());
  }

  @Override
  public void validateActivityParams() {
    //
  }

  @Override
  public boolean deduplicateEvents() {
    return true;
  }

  public static class InfrastructureActivityUpdatableEntity
      extends ActivityUpdatableEntity<InfrastructureActivity, InfrastructureActivity> {
    @Override
    public Class getEntityClass() {
      return InfrastructureActivity.class;
    }

    @Override
    public String getEntityKeyLongString(InfrastructureActivity activity) {
      throw new UnsupportedOperationException("InfrastructureActivity have no unique key");
    }

    @Override
    public Query<InfrastructureActivity> populateKeyQuery(
        Query<InfrastructureActivity> query, InfrastructureActivity changeEvent) {
      throw new UnsupportedOperationException("InfrastructureActivity have no unique key");
    }

    @Override
    public void setUpdateOperations(
        UpdateOperations<InfrastructureActivity> updateOperations, InfrastructureActivity activity) {
      setCommonUpdateOperations(updateOperations, activity);
    }
  }
}
