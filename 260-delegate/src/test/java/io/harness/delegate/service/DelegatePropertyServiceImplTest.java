package io.harness.delegate.service;

import static io.harness.rule.OwnerRule.MATT;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.managerclient.DelegateAgentManagerClient;
import io.harness.managerclient.GetDelegatePropertiesRequest;
import io.harness.managerclient.GetDelegatePropertiesResponse;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import retrofit2.Response;

public class DelegatePropertyServiceImplTest extends CategoryTest {
  private static final String ACCOUNT_ID = "account_id";

  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock private DelegateAgentManagerClient delegateAgentManagerClient;
  @Mock(answer = Answers.RETURNS_DEEP_STUBS) private Call<RestResponse<String>> propertyResponse;

  @InjectMocks @Inject DelegatePropertyServiceImpl propertyService;

  @Before
  public void setUp() throws IOException {
    when(delegateAgentManagerClient.getDelegateProperties(anyString())).thenReturn(propertyResponse);
    doReturn(Response.success(new RestResponse<>(GetDelegatePropertiesResponse.newBuilder().build().toString())))
        .when(propertyResponse)
        .execute();
  }

  @Test
  @Owner(developers = MATT)
  @Category(UnitTests.class)
  public void shouldHitCache() throws ExecutionException {
    GetDelegatePropertiesRequest request = GetDelegatePropertiesRequest.newBuilder().setAccountId(ACCOUNT_ID).build();
    propertyService.getDelegateProperties(request);
    propertyService.getDelegateProperties(request);

    verify(delegateAgentManagerClient, times(1)).getDelegateProperties(anyString());
  }
}
