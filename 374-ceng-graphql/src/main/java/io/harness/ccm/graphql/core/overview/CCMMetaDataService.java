/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ccm.graphql.core.overview;

import io.harness.ccm.commons.dao.CEMetadataRecordDao;
import io.harness.ccm.commons.entities.batch.CEMetadataRecord;
import io.harness.ccm.graphql.dto.overview.CCMMetaData;
import io.harness.ccm.graphql.dto.overview.CCMMetaData.CCMMetaDataBuilder;
import io.harness.ccm.views.dto.DefaultViewIdDto;
import io.harness.ccm.views.service.CEViewService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.NonNull;

@Singleton
public class CCMMetaDataService {
  @Inject private CEMetadataRecordDao metadataRecordDao;
  @Inject private CEViewService ceViewService;

  @NonNull
  public CCMMetaData getCCMMetaData(@NonNull final String accountId) {
    CEMetadataRecord ceMetadataRecord = metadataRecordDao.getByAccountId(accountId);
    CCMMetaDataBuilder ccmMetaDataBuilder = CCMMetaData.builder();
    if (ceMetadataRecord != null) {
      ccmMetaDataBuilder.applicationDataPresent(getFieldBooleanValue(ceMetadataRecord.getApplicationDataPresent()));
      ccmMetaDataBuilder.clusterDataPresent(getFieldBooleanValue(ceMetadataRecord.getClusterDataConfigured()));
      ccmMetaDataBuilder.k8sClusterConnectorPresent(
          getFieldBooleanValue(ceMetadataRecord.getClusterConnectorConfigured()));
      ccmMetaDataBuilder.awsConnectorsPresent(getFieldBooleanValue(ceMetadataRecord.getAwsConnectorConfigured()));
      ccmMetaDataBuilder.gcpConnectorsPresent(getFieldBooleanValue(ceMetadataRecord.getGcpConnectorConfigured()));
      ccmMetaDataBuilder.azureConnectorsPresent(getFieldBooleanValue(ceMetadataRecord.getAzureConnectorConfigured()));
      ccmMetaDataBuilder.cloudDataPresent(isCloudDataPresent(ceMetadataRecord));
    }
    DefaultViewIdDto defaultViewIds = ceViewService.getDefaultViewIds(accountId);
    ccmMetaDataBuilder.defaultAwsPerspectiveId(defaultViewIds.getAwsViewId());
    ccmMetaDataBuilder.defaultAzurePerspectiveId(defaultViewIds.getAzureViewId());
    ccmMetaDataBuilder.defaultGcpPerspectiveId(defaultViewIds.getGcpViewId());
    ccmMetaDataBuilder.defaultClusterPerspectiveId(defaultViewIds.getClusterViewId());

    return ccmMetaDataBuilder.build();
  }

  private Boolean getFieldBooleanValue(Boolean fieldValue) {
    return Boolean.TRUE.equals(fieldValue);
  }

  private boolean isCloudDataPresent(CEMetadataRecord ceMetadataRecord) {
    return Boolean.TRUE.equals(ceMetadataRecord.getAwsDataPresent())
        || Boolean.TRUE.equals(ceMetadataRecord.getGcpDataPresent())
        || Boolean.TRUE.equals(ceMetadataRecord.getAzureDataPresent());
  }
}
