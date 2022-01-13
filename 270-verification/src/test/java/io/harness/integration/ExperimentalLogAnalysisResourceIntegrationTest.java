/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.integration;

import static io.harness.rule.OwnerRule.PRANJAL;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.VerificationIntegrationBase;
import io.harness.category.element.DeprecatedIntegrationTests;
import io.harness.resources.intfc.ExperimentalLogAnalysisResource;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.service.impl.analysis.ExpAnalysisInfo;

import java.net.UnknownHostException;
import java.util.List;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * Created by Pranjal on 09/26/2018
 */
public class ExperimentalLogAnalysisResourceIntegrationTest extends VerificationIntegrationBase {
  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();
  }

  @Test
  @Owner(developers = PRANJAL)
  @Category(DeprecatedIntegrationTests.class)
  @Ignore("TODO: provide proper description why this test is disabled")
  public void testGetLogExpAnalysisInfo() throws UnknownHostException {
    WebTarget getTarget = client.target(VERIFICATION_API_BASE + "/"
        + "learning-exp" + ExperimentalLogAnalysisResource.ANALYSIS_STATE_GET_EXP_ANALYSIS_INFO_URL
        + "?accountId=" + accountId);

    RestResponse<List<ExpAnalysisInfo>> restResponse = getRequestBuilderWithLearningAuthHeader(getTarget).get(
        new GenericType<RestResponse<List<ExpAnalysisInfo>>>() {});

    assertThat(restResponse.getResource().size() <= 1000).isTrue();
  }
}
