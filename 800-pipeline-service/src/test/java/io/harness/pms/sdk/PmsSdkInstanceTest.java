/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk;

import static io.harness.rule.OwnerRule.BRIJESH;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.lock.PersistentLocker;
import io.harness.pms.contracts.plan.InitializeSdkRequest;
import io.harness.pms.pipeline.service.yamlschema.SchemaFetcher;
import io.harness.repositories.sdk.PmsSdkInstanceRepository;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsSdkInstanceTest extends CategoryTest {
  @Mock PmsSdkInstanceRepository pmsSdkInstanceRepository;
  @Mock MongoTemplate mongoTemplate;
  @Mock PersistentLocker persistentLocker;
  @Mock SchemaFetcher schemaFetcher;
  @InjectMocks PmsSdkInstanceService pmsSdkInstanceService;

  @Before
  public void SetUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = BRIJESH)
  @Category(UnitTests.class)
  public void testSaveSdkInstance() {
    InitializeSdkRequest request = InitializeSdkRequest.newBuilder().putStaticAliases("alias", "value").build();
    assertThatCode(() -> pmsSdkInstanceService.saveSdkInstance(request)).doesNotThrowAnyException();
    verify(mongoTemplate, times(1)).findAndModify(any(), any(), (FindAndModifyOptions) any(), any());
  }
}
