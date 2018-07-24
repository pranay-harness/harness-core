package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.ImmutableMap;

import com.amazonaws.services.lambda.AWSLambdaClient;
import com.amazonaws.services.lambda.model.CreateAliasResult;
import com.amazonaws.services.lambda.model.CreateFunctionResult;
import com.amazonaws.services.lambda.model.FunctionConfiguration;
import com.amazonaws.services.lambda.model.GetFunctionResult;
import com.amazonaws.services.lambda.model.InvokeResult;
import com.amazonaws.services.lambda.model.ListAliasesResult;
import com.amazonaws.services.lambda.model.PublishVersionResult;
import com.amazonaws.services.lambda.model.UpdateFunctionCodeResult;
import com.amazonaws.services.lambda.model.UpdateFunctionConfigurationResult;
import org.junit.Test;
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
import software.wings.service.intfc.security.EncryptionService;

import java.nio.charset.StandardCharsets;

public class AwsLambdaHelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsLambdaHelperServiceDelegateImpl awsLambdaHelperServiceDelegate;

  @Test
  public void testExecuteFunction() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), anyString(), any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new InvokeResult().withStatusCode(1).withFunctionError("err").withLogResult("log").withPayload(
                 StandardCharsets.UTF_8.encode("payload")))
        .when(mockClient)
        .invoke(any());
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
  public void testExecuteWf_FxDoesNotExist() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), anyString(), any());
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
    awsLambdaHelperServiceDelegate.executeWf(request, mockCallBack);
    verify(mockClient).createFunction(any());
    verify(mockClient).createAlias(any());
  }

  @Test
  public void testExecuteWf_FxExists() {
    AWSLambdaClient mockClient = mock(AWSLambdaClient.class);
    doReturn(mockClient).when(awsLambdaHelperServiceDelegate).getAmazonLambdaClient(anyString(), anyString(), any());
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
    awsLambdaHelperServiceDelegate.executeWf(request, mockCallBack);
    verify(mockClient, times(2)).updateFunctionCode(any());
    verify(mockClient).updateFunctionConfiguration(any());
    verify(mockClient).publishVersion(any());
    verify(mockClient).createAlias(any());
  }
}