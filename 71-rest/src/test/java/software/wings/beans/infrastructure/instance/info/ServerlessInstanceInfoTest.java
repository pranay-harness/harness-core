package software.wings.beans.infrastructure.instance.info;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;

import java.time.Instant;
import java.util.Collections;

public class ServerlessInstanceInfoTest extends CategoryTest {
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testEquals() {
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo1 = getAwsLambdaInstanceInfo1();
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo2 = getAwsLambdaInstanceInfo2();

    final AwsLambdaInstanceInfo awsLambdaInstanceInfo3 =
        AwsLambdaInstanceInfo.builder()
            .invocationCountList(Collections.singletonList(InvocationCount.builder()
                                                               .count(1)
                                                               .key(InvocationCountKey.LAST_30_DAYS)
                                                               .from(Instant.now().minusSeconds(10))
                                                               .to(Instant.now())
                                                               .build()))
            .build();
    Assertions.assertThat(awsLambdaInstanceInfo1).isEqualTo(awsLambdaInstanceInfo2);
    Assertions.assertThat(awsLambdaInstanceInfo1).isNotEqualTo(awsLambdaInstanceInfo3);
    Assertions.assertThat(awsLambdaInstanceInfo1).isEqualTo(awsLambdaInstanceInfo1);
    Assertions.assertThat(awsLambdaInstanceInfo1).isNotEqualTo(null);
  }

  private AwsLambdaInstanceInfo getAwsLambdaInstanceInfo1() {
    return AwsLambdaInstanceInfo.builder()
        .invocationCountList(Collections.singletonList(InvocationCount.builder()
                                                           .count(10)
                                                           .key(InvocationCountKey.LAST_30_DAYS)
                                                           .from(Instant.now().minusSeconds(1000))
                                                           .to(Instant.now())
                                                           .build()))
        .build();
  }

  private AwsLambdaInstanceInfo getAwsLambdaInstanceInfo2() {
    return AwsLambdaInstanceInfo.builder()
        .invocationCountList(Collections.singletonList(InvocationCount.builder()
                                                           .count(10)
                                                           .key(InvocationCountKey.LAST_30_DAYS)
                                                           .from(Instant.now().minusSeconds(10))
                                                           .to(Instant.now())
                                                           .build()))
        .build();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testHashCode() {
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo1 = getAwsLambdaInstanceInfo1();
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo2 = getAwsLambdaInstanceInfo2();

    Assertions.assertThat(awsLambdaInstanceInfo1.hashCode()).isEqualTo(awsLambdaInstanceInfo2.hashCode());
    Assertions.assertThat(awsLambdaInstanceInfo1.hashCode()).isEqualTo(awsLambdaInstanceInfo1.hashCode());
  }
}