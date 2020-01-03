package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.AliasConfiguration;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionRequest;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesRequest;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.ResourceNotFoundException;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.exception.WingsException;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.service.impl.aws.model.AwsLambdaExecuteFunctionRequest;
import software.wings.service.impl.aws.model.AwsLambdaExecuteWfRequest;
import software.wings.service.impl.aws.model.AwsLambdaFunctionParams;
import software.wings.service.impl.aws.model.AwsLambdaVpcConfig;
import software.wings.service.impl.aws.model.request.AwsLambdaDetailsRequest;
import software.wings.service.impl.aws.model.response.AwsLambdaDetailsResponse;
import software.wings.service.intfc.security.EncryptionService;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class AwsLambdaHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsLambdaHelperServiceDelegateImpl awsLambdaHelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteFunction() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new InvokeResult().withStatusCode(1).withFunctionError("err").withLogResult("log").withPayload(
                 StandardCharsets.UTF_8.encode("payload")))
        .when(mockClient)
        .invoke(any());
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    awsLambdaHelperServiceDelegate.executeFunction(AwsLambdaExecuteFunctionRequest.builder()
                                                       .awsConfig(AwsConfig.builder().build())
                                                       .encryptionDetails(emptyList())
                                                       .region("us-east-1")
                                                       .functionName("fxName")
                                                       .payload("payload")
                                                       .qualifier("qual")
                                                       .build());
    verify(mockClient).invoke(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteWf_FxDoesNotExist() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    ExecutionLogCallback mockCallBack = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallBack).saveExecutionLog(anyString(), any());
    doReturn(null).when(mockClient).getFunction(any());
    doReturn(new CreateFunctionResult()
                 .withCodeSha256("sha256")
                 .withVersion("version")
                 .withFunctionArn("arn")
                 .withFunctionName("name"))
        .when(mockClient)
        .createFunction(any());
    doReturn(new CreateAliasResult().withName("aliasName").withAliasArn("aliasArn"))
        .when(mockClient)
        .createAlias(any());
    AwsLambdaExecuteWfRequest request =
        AwsLambdaExecuteWfRequest.builder()
            .awsConfig(AwsConfig.builder().build())
            .encryptionDetails(emptyList())
            .region("use-east-1")
            .roleArn("arn")
            .evaluatedAliases(singletonList("eval"))
            .serviceVariables(ImmutableMap.of("k1", "v1"))
            .lambdaVpcConfig(AwsLambdaVpcConfig.builder().build())
            .functionParams(singletonList(AwsLambdaFunctionParams.builder().functionName("fxName").build()))
            .build();
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    awsLambdaHelperServiceDelegate.executeWf(request, mockCallBack);
    verify(mockClient).createFunction(any());
    verify(mockClient).createAlias(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testExecuteWf_FxExists() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    ExecutionLogCallback mockCallBack = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallBack).saveExecutionLog(anyString(), any());
    doReturn(new GetFunctionResult().withConfiguration(new FunctionConfiguration().withCodeSha256("sha256_old")))
        .when(mockClient)
        .getFunction(any());
    doReturn(new UpdateFunctionCodeResult().withCodeSha256("sha256_new").withFunctionArn("new-arn"))
        .when(mockClient)
        .updateFunctionCode(any());
    doReturn(new UpdateFunctionConfigurationResult().withFunctionName("name").withCodeSha256("sha256_new"))
        .when(mockClient)
        .updateFunctionConfiguration(any());
    doReturn(new PublishVersionResult().withVersion("version").withFunctionArn("arn"))
        .when(mockClient)
        .publishVersion(any());
    doReturn(new ListAliasesResult().withAliases(emptyList())).when(mockClient).listAliases(any());
    doReturn(new CreateAliasResult().withName("aliasName").withAliasArn("aliasArn"))
        .when(mockClient)
        .createAlias(any());
    AwsLambdaExecuteWfRequest request = AwsLambdaExecuteWfRequest.builder()
                                            .awsConfig(AwsConfig.builder().build())
                                            .encryptionDetails(emptyList())
                                            .region("use-east-1")
                                            .roleArn("arn")
                                            .evaluatedAliases(singletonList("eval"))
                                            .serviceVariables(ImmutableMap.of("k1", "v1"))
                                            .lambdaVpcConfig(AwsLambdaVpcConfig.builder().build())
                                            .functionParams(singletonList(AwsLambdaFunctionParams.builder().build()))
                                            .build();
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    awsLambdaHelperServiceDelegate.executeWf(request, mockCallBack);
    verify(mockClient, times(2)).updateFunctionCode(any());
    verify(mockClient).updateFunctionConfiguration(any());
    verify(mockClient).publishVersion(any());
    verify(mockClient).createAlias(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testTagExistingFunction() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    String functionArn = "functionArn";
    Map<String, String> initialTags = ImmutableMap.of("k1", "v1");
    GetFunctionResult getFunctionResult =
        new GetFunctionResult()
            .withConfiguration(new FunctionConfiguration().withFunctionArn(functionArn))
            .withTags(initialTags);
    Map<String, String> finalTags = ImmutableMap.of("k2", "v2");
    ExecutionLogCallback mockCallBack = mock(ExecutionLogCallback.class);
    doNothing().when(mockCallBack).saveExecutionLog(anyString());
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    awsLambdaHelperServiceDelegate.tagExistingFunction(getFunctionResult, finalTags, mockCallBack, mockClient);
    verify(mockClient).untagResource(any());
    verify(mockClient).tagResource(any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetAlternateNormalizedFunctionName() {
    assertThat(awsLambdaHelperServiceDelegate.getAlternateNormalizedFunctionName("foo_bar")).isEqualTo("foo-bar");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getFunctionDetails() {
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    final GetFunctionResult getFunctionResult =
        new GetFunctionResult()
            .withConfiguration(new FunctionConfiguration().withLastModified("2019-10-09T11:49:13.585-0700"))
            .withTags(ImmutableMap.of("key", "value"));

    doReturn(getFunctionResult).when(mockClient).getFunction(any(GetFunctionRequest.class));
    final ListAliasesResult listAliasesResult =
        new ListAliasesResult().withAliases(new AliasConfiguration().withName("alias"));
    doReturn(listAliasesResult).when(mockClient).listAliases(any(ListAliasesRequest.class));

    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), any());
    final AwsLambdaDetailsRequest awsLambdaDetailsRequest = AwsLambdaDetailsRequest.builder().loadAliases(true).build();
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    final AwsLambdaDetailsResponse functionDetails =
        awsLambdaHelperServiceDelegate.getFunctionDetails(awsLambdaDetailsRequest);
    assertThat(functionDetails.getLambdaDetails().getLastModified()).isNotNull();
    assertThat(functionDetails.getLambdaDetails().getTags()).containsKeys("key");
    assertThat(functionDetails.getLambdaDetails().getAliases()).contains("alias");
    verify(mockClient, times(1)).listAliases(any(ListAliasesRequest.class));
    verify(mockClient, times(1)).getFunction(any(GetFunctionRequest.class));
  }

  @Test()
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_getFunctionDetails_error() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), any());

    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doThrow(new AmazonServiceException("service exception"))
        .when(mockClient)
        .getFunction(any(GetFunctionRequest.class));

    final AwsLambdaDetailsRequest awsLambdaDetailsRequest = AwsLambdaDetailsRequest.builder().loadAliases(true).build();
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsLambdaHelperServiceDelegate.getFunctionDetails(awsLambdaDetailsRequest));

    doThrow(new AmazonClientException("client exception")).when(mockClient).getFunction(any(GetFunctionRequest.class));
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> awsLambdaHelperServiceDelegate.getFunctionDetails(awsLambdaDetailsRequest));

    doThrow(new ResourceNotFoundException("resource not found"))
        .when(mockClient)
        .getFunction(any(GetFunctionRequest.class));
    doNothing().when(mockTracker).trackLambdaCall(anyString());
    final AwsLambdaDetailsResponse functionDetails =
        awsLambdaHelperServiceDelegate.getFunctionDetails(awsLambdaDetailsRequest);
    assertThat(functionDetails.getLambdaDetails()).isNull();
  }
}