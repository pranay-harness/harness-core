/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.verification;

import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static org.apache.cxf.ws.addressing.ContextUtils.generateUUID;

import software.wings.WingsBaseTest;
import software.wings.sm.StateType;
import software.wings.sm.states.CustomLogVerificationState.LogCollectionInfo;
import software.wings.sm.states.CustomLogVerificationState.Method;
import software.wings.sm.states.CustomLogVerificationState.ResponseMapping;
import software.wings.sm.states.CustomLogVerificationState.ResponseType;
import software.wings.verification.log.CustomLogCVServiceConfiguration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CVConfigurationServiceImplTestBase extends WingsBaseTest {
  public static CustomLogCVServiceConfiguration createCustomLogsConfig(String accountId) throws Exception {
    CustomLogCVServiceConfiguration configuration =
        CustomLogCVServiceConfiguration.builder()
            .logCollectionInfo(LogCollectionInfo.builder()
                                   .collectionUrl("testUrl ${start_time} and ${end_time}")
                                   .method(Method.GET)
                                   .responseType(ResponseType.JSON)
                                   .responseMapping(ResponseMapping.builder()
                                                        .hostJsonPath("hostname")
                                                        .logMessageJsonPath("message")
                                                        .timestampJsonPath("@timestamp")
                                                        .build())
                                   .build())
            .build();

    configuration.setAccountId(accountId);
    configuration.setStateType(StateType.STACK_DRIVER);
    configuration.setEnvId(generateUuid());
    configuration.setName("StackDriver");
    configuration.setConnectorId(generateUuid());
    configuration.setServiceId(generateUuid());
    configuration.setStateType(StateType.LOG_VERIFICATION);
    configuration.setQuery(generateUUID());
    configuration.setBaselineStartMinute(16);
    configuration.setBaselineEndMinute(30);
    return configuration;
  }
}
