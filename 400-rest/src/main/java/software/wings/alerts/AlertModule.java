/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.alerts;

import io.harness.alert.AlertData;

import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.ApprovalNeededAlert;
import software.wings.beans.alert.ArtifactCollectionFailedAlert;
import software.wings.beans.alert.DelegatesDownAlert;
import software.wings.beans.alert.DeploymentFreezeEventAlert;
import software.wings.beans.alert.DeploymentRateApproachingLimitAlert;
import software.wings.beans.alert.EmailSendingFailedAlert;
import software.wings.beans.alert.GitConnectionErrorAlert;
import software.wings.beans.alert.GitSyncErrorAlert;
import software.wings.beans.alert.InstanceUsageLimitAlert;
import software.wings.beans.alert.InvalidSMTPConfigAlert;
import software.wings.beans.alert.KmsSetupAlert;
import software.wings.beans.alert.ManifestCollectionFailedAlert;
import software.wings.beans.alert.ResourceUsageApproachingLimitAlert;
import software.wings.beans.alert.SSOSyncFailedAlert;
import software.wings.beans.alert.SettingAttributeValidationFailedAlert;
import software.wings.beans.alert.UsageLimitExceededAlert;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;

public class AlertModule extends AbstractModule {
  private static volatile AlertModule instance;

  public static AlertModule getInstance() {
    if (instance == null) {
      instance = new AlertModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bindAlerts();
  }

  private void bindAlerts() {
    MapBinder<AlertType, Class<? extends AlertData>> mapBinder = MapBinder.newMapBinder(
        binder(), new TypeLiteral<AlertType>() {}, new TypeLiteral<Class<? extends AlertData>>() {});

    mapBinder.addBinding(AlertType.ApprovalNeeded).toInstance(ApprovalNeededAlert.class);
    mapBinder.addBinding(AlertType.ManualInterventionNeeded).toInstance(AlertData.class);
    mapBinder.addBinding(AlertType.DelegatesDown).toInstance(DelegatesDownAlert.class);
    mapBinder.addBinding(AlertType.InvalidKMS).toInstance(KmsSetupAlert.class);
    mapBinder.addBinding(AlertType.GitSyncError).toInstance(GitSyncErrorAlert.class);
    mapBinder.addBinding(AlertType.GitConnectionError).toInstance(GitConnectionErrorAlert.class);
    mapBinder.addBinding(AlertType.INVALID_SMTP_CONFIGURATION).toInstance(InvalidSMTPConfigAlert.class);
    mapBinder.addBinding(AlertType.EMAIL_NOT_SENT_ALERT).toInstance(EmailSendingFailedAlert.class);
    mapBinder.addBinding(AlertType.USERGROUP_SYNC_FAILED).toInstance(SSOSyncFailedAlert.class);
    mapBinder.addBinding(AlertType.USAGE_LIMIT_EXCEEDED).toInstance(UsageLimitExceededAlert.class);
    mapBinder.addBinding(AlertType.INSTANCE_USAGE_APPROACHING_LIMIT).toInstance(InstanceUsageLimitAlert.class);
    mapBinder.addBinding(AlertType.RESOURCE_USAGE_APPROACHING_LIMIT)
        .toInstance(ResourceUsageApproachingLimitAlert.class);
    mapBinder.addBinding(AlertType.DEPLOYMENT_RATE_APPROACHING_LIMIT)
        .toInstance(DeploymentRateApproachingLimitAlert.class);
    mapBinder.addBinding(AlertType.SETTING_ATTRIBUTE_VALIDATION_FAILED)
        .toInstance(SettingAttributeValidationFailedAlert.class);
    mapBinder.addBinding(AlertType.ARTIFACT_COLLECTION_FAILED).toInstance(ArtifactCollectionFailedAlert.class);
    mapBinder.addBinding(AlertType.CONTINUOUS_VERIFICATION_ALERT).toInstance(ContinuousVerificationAlertData.class);
    mapBinder.addBinding(AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT)
        .toInstance(ContinuousVerificationDataCollectionAlert.class);
    mapBinder.addBinding(AlertType.MANIFEST_COLLECTION_FAILED).toInstance(ManifestCollectionFailedAlert.class);
    mapBinder.addBinding(AlertType.DEPLOYMENT_FREEZE_EVENT).toInstance(DeploymentFreezeEventAlert.class);
  }
}
