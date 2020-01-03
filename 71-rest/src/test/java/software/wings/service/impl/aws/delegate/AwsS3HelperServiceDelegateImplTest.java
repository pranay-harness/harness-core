package software.wings.service.impl.aws.delegate;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static wiremock.com.google.common.collect.Lists.newArrayList;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import io.harness.aws.AwsCallTracker;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsS3HelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Mock private AwsCallTracker mockTracker;
  @Spy @InjectMocks private AwsS3HelperServiceDelegateImpl s3HelperServiceDelegate;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListBucketNames() {
    AmazonS3Client mockClient = mock(AmazonS3Client.class);
    doReturn(mockClient).when(s3HelperServiceDelegate).getAmazonS3Client(any());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doNothing().when(mockTracker).trackS3Call(anyString());
    Bucket b_00 = new Bucket();
    b_00.setName("name_00");
    Bucket b_01 = new Bucket();
    b_01.setName("name_01");
    doReturn(newArrayList(b_00, b_01)).when(mockClient).listBuckets();
    List<String> names = s3HelperServiceDelegate.listBucketNames(AwsConfig.builder().build(), emptyList());
    assertThat(names).isNotNull();
    assertThat(names.size()).isEqualTo(2);
    assertThat(names.contains("name_00")).isTrue();
    assertThat(names.contains("name_01")).isTrue();
  }
}