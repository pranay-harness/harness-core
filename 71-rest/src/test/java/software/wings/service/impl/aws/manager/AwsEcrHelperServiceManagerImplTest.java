package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.APP_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsEcrGetAuthTokenResponse;
import software.wings.service.impl.aws.model.AwsEcrGetImageUrlResponse;
import software.wings.service.intfc.DelegateService;

public class AwsEcrHelperServiceManagerImplTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetAmazonEcrAuthToken() throws InterruptedException {
    AwsEcrHelperServiceManagerImpl service = spy(AwsEcrHelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsEcrGetAuthTokenResponse.builder().ecrAuthToken("token").build())
        .when(mockDelegateService)
        .executeTask(any());
    String token = service.getAmazonEcrAuthToken(AwsConfig.builder().build(), emptyList(), "aws", "us-east-1", APP_ID);
    assertThat(token).isEqualTo("token");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetEcrImageUrl() throws InterruptedException {
    AwsEcrHelperServiceManagerImpl service = spy(AwsEcrHelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsEcrGetImageUrlResponse.builder().ecrImageUrl("url").build())
        .when(mockDelegateService)
        .executeTask(any());
    String url = service.getEcrImageUrl(AwsConfig.builder().build(), emptyList(), "us-east-1", "img", APP_ID);
    assertThat(url).isEqualTo("url");
  }
}