package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsS3ListBucketNamesResponse;
import software.wings.service.intfc.DelegateService;

import java.util.List;

public class AwsS3HelperServiceManagerImplTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListBucketNames() throws InterruptedException {
    AwsS3HelperServiceManagerImpl service = spy(AwsS3HelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsS3ListBucketNamesResponse.builder().bucketNames(asList("b-0", "b-1")).build())
        .when(mockDelegateService)
        .executeTask(any());
    List<String> names = service.listBucketNames(AwsConfig.builder().build(), emptyList());
    assertThat(names).isNotNull();
    assertThat(names.size()).isEqualTo(2);
    assertThat(names.contains("b-0")).isTrue();
    assertThat(names.contains("b-1")).isTrue();
  }
}