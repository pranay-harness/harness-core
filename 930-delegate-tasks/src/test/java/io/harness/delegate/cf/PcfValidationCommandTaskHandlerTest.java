/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.cf;

import static io.harness.delegate.cf.CfTestConstants.ACCOUNT_ID;
import static io.harness.rule.OwnerRule.ANSHUL;

import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.pcf.CfInternalConfig;
import io.harness.delegate.task.pcf.request.CfInfraMappingDataRequest;
import io.harness.pcf.CfDeploymentManager;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.CDP)
public class PcfValidationCommandTaskHandlerTest extends CategoryTest {
  @Mock private CfDeploymentManager pcfDeploymentManager;
  @Mock private SecretDecryptionService encryptionService;
  @Mock private ILogStreamingTaskClient logStreamingTaskClient;

  @InjectMocks @Inject private PcfValidationCommandTaskHandler pcfValidationCommandTaskHandler;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = ANSHUL)
  @Category(UnitTests.class)
  public void TestDecryptionOfPcfConfig() {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    CfInternalConfig pcfConfig =
        CfInternalConfig.builder().accountId(ACCOUNT_ID).password("password".toCharArray()).build();

    pcfValidationCommandTaskHandler.executeTaskInternal(
        CfInfraMappingDataRequest.builder().pcfConfig(pcfConfig).build(), encryptedDataDetails, logStreamingTaskClient,
        false);

    verify(encryptionService).decrypt(pcfConfig, encryptedDataDetails, false);
  }
}
