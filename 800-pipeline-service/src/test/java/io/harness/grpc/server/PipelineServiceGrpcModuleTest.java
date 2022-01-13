/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.grpc.server;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.ModuleType;
import io.harness.PipelineServiceConfiguration;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.grpc.client.GrpcClientConfig;
import io.harness.pms.contracts.governance.JsonExpansionServiceGrpc.JsonExpansionServiceBlockingStub;
import io.harness.rule.Owner;

import java.util.HashMap;
import java.util.Map;
import javax.net.ssl.SSLException;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PipelineServiceGrpcModuleTest extends CategoryTest {
  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetJsonExpansionHandlerClients() throws SSLException {
    PipelineServiceConfiguration configuration = new PipelineServiceConfiguration();
    GrpcClientConfig client1 = GrpcClientConfig.builder().target("t1").authority("a1").build();
    GrpcClientConfig client2 = GrpcClientConfig.builder().target("t2").authority("a2").build();
    Map<String, GrpcClientConfig> grpcClientConfigMap = new HashMap<>();
    grpcClientConfigMap.put("CD", client1);
    grpcClientConfigMap.put("CI", client2);
    configuration.setGrpcClientConfigs(grpcClientConfigMap);

    PipelineServiceGrpcModule pipelineServiceGrpcModule = PipelineServiceGrpcModule.getInstance();
    Map<ModuleType, JsonExpansionServiceBlockingStub> jsonExpansionHandlerClients =
        pipelineServiceGrpcModule.getJsonExpansionHandlerClients(configuration);
    assertThat(jsonExpansionHandlerClients).hasSize(3);
    assertThat(jsonExpansionHandlerClients).containsOnlyKeys(ModuleType.CD, ModuleType.CI, ModuleType.PMS);

    grpcClientConfigMap.clear();
    pipelineServiceGrpcModule = PipelineServiceGrpcModule.getInstance();
    jsonExpansionHandlerClients = pipelineServiceGrpcModule.getJsonExpansionHandlerClients(configuration);
    assertThat(jsonExpansionHandlerClients).hasSize(1);
    assertThat(jsonExpansionHandlerClients).containsOnlyKeys(ModuleType.PMS);
  }
}
