/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.aws.delegate;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.security.EncryptionService;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;

@OwnedBy(CDP)
public class AwsCFHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsCFHelperServiceDelegateImpl awsCFHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetParamsData() {
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList(), eq(false));
    doReturn(mockClient).when(awsCFHelperServiceDelegate).getAmazonCloudFormationClient(any(), any());
    doReturn(new GetTemplateSummaryResult().withParameters(
                 new ParameterDeclaration().withParameterKey("k1").withParameterType("t1").withDefaultValue("d1"),
                 new ParameterDeclaration().withParameterKey("k2").withParameterType("t2").withDefaultValue("d2")))
        .when(mockClient)
        .getTemplateSummary(any());
    doNothing().when(mockTracker).trackCFCall(anyString());
    List<AwsCFTemplateParamsData> paramsData = awsCFHelperServiceDelegate.getParamsData(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "url", "body", null, null, null);
    assertThat(paramsData).isNotNull();
    assertThat(paramsData.size()).isEqualTo(2);
    verifyParamsData(paramsData.get(0), "k1", "t1", "d1");
    verifyParamsData(paramsData.get(1), "k2", "t2", "d2");
  }

  private void verifyParamsData(AwsCFTemplateParamsData data, String key, String type, String defaultVal) {
    assertThat(data).isNotNull();
    assertThat(data.getParamKey()).isEqualTo(key);
    assertThat(data.getParamType()).isEqualTo(type);
    assertThat(data.getDefaultValue()).isEqualTo(defaultVal);
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetStackBody() {
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    doReturn(mockClient).when(awsCFHelperServiceDelegate).getAmazonCloudFormationClient(any(), any());
    doReturn(new GetTemplateResult().withTemplateBody("body")).when(mockClient).getTemplate(any());
    doNothing().when(mockTracker).trackCFCall(anyString());
    String body = awsCFHelperServiceDelegate.getStackBody(AwsConfig.builder().build(), "us-east-1", "stackId");
    assertThat(body).isEqualTo("body");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetCapabilities() {
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    doReturn(mockClient).when(awsCFHelperServiceDelegate).getAmazonCloudFormationClient(any(), any());
    doReturn(new GetTemplateSummaryResult().withCapabilities("c1", "c2")).when(mockClient).getTemplateSummary(any());
    doNothing().when(mockTracker).trackCFCall(anyString());
    List<String> capabilities =
        awsCFHelperServiceDelegate.getCapabilities(AwsConfig.builder().build(), "us-east-1", "foo", "body");
    assertThat(capabilities).isNotNull();
    assertThat(capabilities.size()).isEqualTo(2);
    assertThat(capabilities.get(0)).isEqualTo("c1");
    assertThat(capabilities.get(1)).isEqualTo("c2");
  }
}
