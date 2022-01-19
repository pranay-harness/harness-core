/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.delegate.task.cvng;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.task.ConnectorValidationHandler;
import io.harness.datacollection.DataCollectionDSLService;
import io.harness.datacollection.entity.RuntimeParameters;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.ConnectorValidationParams;
import io.harness.delegate.beans.connector.cvconnector.CVConnectorValidationParams;
import io.harness.delegate.beans.cvng.ConnectorValidationInfo;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Clock;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.CV)
public class CVConnectorValidationHandler implements ConnectorValidationHandler {
  @Inject SecretDecryptionService secretDecryptionService;
  @Inject private Clock clock;
  @Inject private DataCollectionDSLService dataCollectionDSLService;
  @Inject private NGErrorHelper ngErrorHelper;

  @Override
  public ConnectorValidationResult validate(
      ConnectorValidationParams connectorValidationParams, String accountIdentifier) {
    final CVConnectorValidationParams cvConnectorValidationParams =
        (CVConnectorValidationParams) connectorValidationParams;
    final ConnectorConfigDTO connectorConfigDTO = cvConnectorValidationParams.getConnectorConfigDTO();

    if (connectorConfigDTO instanceof DecryptableEntity) {
      List<DecryptableEntity> decryptableEntities = connectorConfigDTO.getDecryptableEntities();
      List<List<EncryptedDataDetail>> encryptedDataDetails = cvConnectorValidationParams.getEncryptedDataDetails();

      if (isNotEmpty(decryptableEntities)) {
        for (int decryptableEntityIndex = 0; decryptableEntityIndex < decryptableEntities.size();
             decryptableEntityIndex++) {
          DecryptableEntity decryptableEntity = decryptableEntities.get(decryptableEntityIndex);
          List<EncryptedDataDetail> encryptedDataDetail = encryptedDataDetails.get(decryptableEntityIndex);
          secretDecryptionService.decrypt(decryptableEntity, encryptedDataDetail);
        }
      }
    }

    ConnectorValidationResult result = null;
    boolean validCredentials = false;
    try {
      ConnectorValidationInfo connectorValidationInfo =
          ConnectorValidationInfo.getConnectorValidationInfo(connectorConfigDTO);
      String dsl = connectorValidationInfo.getConnectionValidationDSL();
      Instant now = clock.instant();
      final RuntimeParameters runtimeParameters = RuntimeParameters.builder()
                                                      .baseUrl(connectorValidationInfo.getBaseUrl())
                                                      .commonHeaders(connectorValidationInfo.collectionHeaders())
                                                      .commonOptions(connectorValidationInfo.collectionParams())
                                                      .otherEnvVariables(connectorValidationInfo.getDslEnvVariables())
                                                      .endTime(connectorValidationInfo.getEndTime(now))
                                                      .startTime(connectorValidationInfo.getStartTime(now))
                                                      .build();
      validCredentials = ((String) dataCollectionDSLService.execute(dsl, runtimeParameters)).equalsIgnoreCase("true");
      if (validCredentials) {
        result = ConnectorValidationResult.builder()
                     .status(ConnectivityStatus.SUCCESS)
                     .testedAt(System.currentTimeMillis())
                     .build();
      } else {
        String errorMessage = "Credentials are not valid";
        result = ConnectorValidationResult.builder()
                     .status(ConnectivityStatus.FAILURE)
                     .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
                     .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                     .testedAt(System.currentTimeMillis())
                     .build();
      }
    } catch (Exception ex) {
      String errorMessage = ex.getMessage();
      result = ConnectorValidationResult.builder()
                   .status(ConnectivityStatus.FAILURE)
                   .errors(Collections.singletonList(ngErrorHelper.createErrorDetail(errorMessage)))
                   .errorSummary(ngErrorHelper.getErrorSummary(errorMessage))
                   .testedAt(System.currentTimeMillis())
                   .build();
    }
    return result;
  }
}
