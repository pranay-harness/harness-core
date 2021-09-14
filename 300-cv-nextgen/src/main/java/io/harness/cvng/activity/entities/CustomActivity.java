/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.activity.entities;

import io.harness.cvng.beans.activity.ActivityDTO;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.verificationjob.entities.VerificationJobInstance.VerificationJobInstanceBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Builder
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("CUSTOM")
public class CustomActivity extends Activity {
  @Override
  public ActivityType getType() {
    return ActivityType.CUSTOM;
  }

  @Override
  public void fromDTO(ActivityDTO activityDTO) {
    setType(ActivityType.CUSTOM);
    addCommonFields(activityDTO);
  }

  @Override
  public void fillInVerificationJobInstanceDetails(VerificationJobInstanceBuilder verificationJobInstance) {}

  @Override
  public void validateActivityParams() {}

  @Override
  public boolean deduplicateEvents() {
    throw new UnsupportedOperationException("Custom events are not yet supported");
  }

  public static class CustomActivityUpdatableEntity extends ActivityUpdatableEntity<CustomActivity, CustomActivity> {
    @Override
    public Class getEntityClass() {
      return CustomActivity.class;
    }

    public Query<CustomActivity> populateKeyQuery(Query<CustomActivity> query, HarnessCDActivity changeEvent) {
      throw new UnsupportedOperationException("Custom events have no unique key");
    }

    @Override
    public void setUpdateOperations(UpdateOperations<CustomActivity> updateOperations, CustomActivity activity) {
      setCommonUpdateOperations(updateOperations, activity);
    }
  }
}
