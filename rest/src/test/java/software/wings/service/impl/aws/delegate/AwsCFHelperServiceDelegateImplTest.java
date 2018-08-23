package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.cloudformation.AmazonCloudFormationClient;
import com.amazonaws.services.cloudformation.model.GetTemplateResult;
import com.amazonaws.services.cloudformation.model.GetTemplateSummaryResult;
import com.amazonaws.services.cloudformation.model.ParameterDeclaration;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsCFTemplateParamsData;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsCFHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsCFHelperServiceDelegateImpl awsCFHelperServiceDelegate;

  @Test
  public void testGetParamsData() {
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(mockClient).when(awsCFHelperServiceDelegate).getAmazonCloudFormationClient(any(), anyString(), any());
    doReturn(new GetTemplateSummaryResult().withParameters(
                 new ParameterDeclaration().withParameterKey("k1").withParameterType("t1").withDefaultValue("d1"),
                 new ParameterDeclaration().withParameterKey("k2").withParameterType("t2").withDefaultValue("d2")))
        .when(mockClient)
        .getTemplateSummary(any());
    List<AwsCFTemplateParamsData> paramsData =
        awsCFHelperServiceDelegate.getParamsData(AwsConfig.builder().build(), emptyList(), "us-east-1", "url", "body");
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
  public void testGetStackBody() {
    AmazonCloudFormationClient mockClient = mock(AmazonCloudFormationClient.class);
    doReturn(mockClient).when(awsCFHelperServiceDelegate).getAmazonCloudFormationClient(any(), anyString(), any());
    doReturn(new GetTemplateResult().withTemplateBody("body")).when(mockClient).getTemplate(any());
    String body = awsCFHelperServiceDelegate.getStackBody(AwsConfig.builder().build(), "us-east-1", "stackId");
    assertThat(body).isEqualTo("body");
  }
}
