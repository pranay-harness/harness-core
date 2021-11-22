package io.harness.cvng.activity.services.impl;

import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.activity.services.api.ActivityService;
import io.harness.cvng.activity.services.api.ActivityUpdateHandler;

import com.google.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;

public class DeploymentActivityUpdateHandler extends ActivityUpdateHandler<DeploymentActivity> {
  @Inject ActivityService activityService;

  @Override
  public void handleCreate(DeploymentActivity activity) {}

  @Override
  public void handleDelete(DeploymentActivity activity) {}

  @Override
  public void handleUpdate(DeploymentActivity existingActivity, DeploymentActivity newActivity) {
    if (CollectionUtils.isNotEmpty(existingActivity.getVerificationJobInstanceIds())) {
      activityService.updateActivityStatus(existingActivity);
    }
  }
}
