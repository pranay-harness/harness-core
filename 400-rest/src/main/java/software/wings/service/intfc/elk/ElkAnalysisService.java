/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.intfc.elk;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.ElkConfig;
import software.wings.service.impl.analysis.VerificationNodeDataSetupResponse;
import software.wings.service.impl.elk.ElkIndexTemplate;
import software.wings.service.impl.elk.ElkSetupTestNodeData;
import software.wings.service.intfc.analysis.AnalysisService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Created by rsingh on 8/23/17.
 */
public interface ElkAnalysisService extends AnalysisService {
  Map<String, ElkIndexTemplate> getIndices(String accountId, String analysisServerConfigId) throws IOException;

  String getVersion(String accountId, ElkConfig elkConfig, List<EncryptedDataDetail> encryptedDataDetails)
      throws IOException;

  /**
   * method to get log data based on host provided.
   * @param accountId
   * @param elkSetupTestNodeData
   * @return
   */
  VerificationNodeDataSetupResponse getLogDataByHost(String accountId, ElkSetupTestNodeData elkSetupTestNodeData);

  Boolean validateQuery(String accountId, String appId, String settingId, String query, String index, String guid,
      String hostnameField, String messageField, String timestampField);
}
