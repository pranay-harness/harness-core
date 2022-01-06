/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.scalyr;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ScalyrConfig;
import software.wings.service.intfc.scalyr.ScalyrService;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapper;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ScalyrServiceImpl implements ScalyrService {
  @Override
  public Map<String, Map<String, ResponseMapper>> createLogCollectionMapping(
      String hostnameField, String messageField, String timestampField) {
    Map<String, Map<String, ResponseMapper>> logCollectionMapping = new HashMap<>();
    Map<String, ResponseMapper> responseMap = new HashMap<>();
    responseMap.put(
        "host", ResponseMapper.builder().fieldName("host").jsonPath(Collections.singletonList(hostnameField)).build());
    responseMap.put("timestamp",
        ResponseMapper.builder().fieldName("timestamp").jsonPath(Collections.singletonList(timestampField)).build());
    responseMap.put("logMessage",
        ResponseMapper.builder().fieldName("logMessage").jsonPath(Collections.singletonList(messageField)).build());
    logCollectionMapping.put(ScalyrConfig.QUERY_URL, responseMap);

    return logCollectionMapping;
  }
}
