/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.migrations.all;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.migrations.Migration;

import software.wings.beans.alert.Alert;
import software.wings.beans.alert.Alert.AlertKeys;
import software.wings.beans.alert.AlertType;
import software.wings.beans.alert.cv.ContinuousVerificationAlertData;
import software.wings.beans.alert.cv.ContinuousVerificationDataCollectionAlert;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.verification.CVConfigurationService;
import software.wings.verification.CVConfiguration;

import com.google.inject.Inject;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteServiceGuardAlertMigration implements Migration {
  @Inject AlertService alertService;
  @Inject CVConfigurationService cvConfigurationService;

  @Override
  public void migrate() {
    PageResponse<Alert> pageResponse =
        alertService.list(PageRequestBuilder.aPageRequest()
                              .addFilter(AlertKeys.type, Operator.IN,
                                  Arrays
                                      .asList(AlertType.CONTINUOUS_VERIFICATION_ALERT,
                                          AlertType.CONTINUOUS_VERIFICATION_DATA_COLLECTION_ALERT)
                                      .toArray())
                              .build());

    if (pageResponse == null) {
      log.info("No CV alerts found to migrate");
      return;
    }
    List<Alert> dataCollectionAlertList = pageResponse.getResponse();

    Set<String> deletedCVConfigs = new HashSet<>();
    if (isNotEmpty(dataCollectionAlertList)) {
      log.info("Going through {} alerts to find which ones to delete", dataCollectionAlertList.size());
      dataCollectionAlertList.forEach(alert -> {
        CVConfiguration cvConfigurationInAlert = null;
        if (alert.getType() == AlertType.CONTINUOUS_VERIFICATION_ALERT) {
          cvConfigurationInAlert = ((ContinuousVerificationAlertData) alert.getAlertData()).getCvConfiguration();
        } else {
          cvConfigurationInAlert =
              ((ContinuousVerificationDataCollectionAlert) alert.getAlertData()).getCvConfiguration();
        }

        CVConfiguration cvConfiguration = null;

        if (!deletedCVConfigs.contains(cvConfigurationInAlert.getUuid())) {
          cvConfiguration = cvConfigurationService.getConfiguration(cvConfigurationInAlert.getUuid());
        }

        if (cvConfiguration == null) {
          deletedCVConfigs.add(cvConfigurationInAlert.getUuid());
          alertService.closeAlert(
              cvConfigurationInAlert.getAccountId(), alert.getAppId(), alert.getType(), alert.getAlertData());
        }
      });
    }
  }
}
