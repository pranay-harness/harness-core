package io.harness.cvng.core.services.impl;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;

import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.core.beans.DSConfig;
import io.harness.cvng.core.beans.DSConfig.CVConfigUpdateResult;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.CVConfigTransformer;
import io.harness.cvng.core.services.api.DSConfigService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DSConfigServiceImpl implements DSConfigService {
  @Inject CVConfigService cvConfigService;
  @Inject private Injector injector;
  @Override
  public List<DSConfig> list(String accountId, String connectorId, String productName) {
    List<CVConfig> cvConfigs = cvConfigService.list(accountId, connectorId, productName);
    if (cvConfigs.isEmpty()) {
      return Collections.emptyList();
    }
    DataSourceType dataSourceType = cvConfigs.get(0).getType();
    CVConfigTransformer<? extends CVConfig, ? extends DSConfig> cvConfigTransformer =
        injector.getInstance(Key.get(CVConfigTransformer.class, Names.named(dataSourceType.name())));
    Map<String, List<CVConfig>> groupById =
        cvConfigs.stream().collect(Collectors.groupingBy(CVConfig::getGroupId, Collectors.toList()));
    return groupById.values().stream().map(group -> cvConfigTransformer.transform(group)).collect(Collectors.toList());
  }

  @Override
  public void upsert(DSConfig dsConfig) {
    List<CVConfig> saved = cvConfigService.list(
        dsConfig.getAccountId(), dsConfig.getConnectorId(), dsConfig.getProductName(), dsConfig.getIdentifier());
    CVConfigUpdateResult cvConfigUpdateResult = dsConfig.getCVConfigUpdateResult(saved);
    cvConfigService.delete(
        cvConfigUpdateResult.getDeleted().stream().map(cvConfig -> cvConfig.getUuid()).collect(Collectors.toList()));
    cvConfigService.update(cvConfigUpdateResult.getUpdated());
    cvConfigService.save(cvConfigUpdateResult.getAdded());
  }

  @Override
  public void delete(String accountId, String connectorId, String productName, String identifier) {
    cvConfigService.deleteByGroupId(accountId, connectorId, productName, identifier);
  }
}
