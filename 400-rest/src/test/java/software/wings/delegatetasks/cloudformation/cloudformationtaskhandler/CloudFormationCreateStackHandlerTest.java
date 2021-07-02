package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.PRAKHAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.service.intfc.aws.delegate.AwsCFHelperServiceDelegate;

import com.amazonaws.services.cloudformation.model.Tag;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class CloudFormationCreateStackHandlerTest extends WingsBaseTest {
  @Mock private AwsCFHelperServiceDelegate awsCFHelperServiceDelegate;
  @InjectMocks private CloudFormationCreateStackHandler cloudFormationCreateStackHandler;

  @Test
  @Owner(developers = PRAKHAR)
  @Category(UnitTests.class)
  public void testGetCloudformationTags() throws IOException {
    CloudFormationCreateStackRequest cloudFormationCreateStackRequest =
        CloudFormationCreateStackRequest.builder().build();
    assertThat(cloudFormationCreateStackHandler.getCloudformationTags(cloudFormationCreateStackRequest)).isNull();

    cloudFormationCreateStackRequest.setTags("");
    assertThat(cloudFormationCreateStackHandler.getCloudformationTags(cloudFormationCreateStackRequest)).isNull();

    cloudFormationCreateStackRequest.setTags("[]");
    assertThat(cloudFormationCreateStackHandler.getCloudformationTags(cloudFormationCreateStackRequest))
        .isEqualTo(new ArrayList<Tag>());

    cloudFormationCreateStackRequest.setTags(
        "[{\r\n\t\"key\": \"tagKey1\",\r\n\t\"value\": \"tagValue1\"\r\n}, {\r\n\t\"key\": \"tagKey2\",\r\n\t\"value\": \"tagValue2\"\r\n}]");
    List<Tag> expectedTags = Arrays.asList(
        new Tag().withKey("tagKey1").withValue("tagValue1"), new Tag().withKey("tagKey2").withValue("tagValue2"));
    assertThat(cloudFormationCreateStackHandler.getCloudformationTags(cloudFormationCreateStackRequest))
        .isEqualTo(expectedTags);
  }

  @Test
  @Owner(developers = PRAKHAR)
  @Category(UnitTests.class)
  public void testGetCapabilities() throws IOException {
    List<String> capabilitiesByTemplateSummary = Arrays.asList("CAPABILITY_IAM", "CAPABILITY_AUTO_EXPAND");
    List<String> userDefinedCapabilities = Collections.singletonList("CAPABILITY_AUTO_EXPAND");
    doReturn(capabilitiesByTemplateSummary)
        .when(awsCFHelperServiceDelegate)
        .getCapabilities(any(AwsConfig.class), anyString(), anyString(), anyString());

    List<String> expectedCapabilities = Arrays.asList("CAPABILITY_IAM", "CAPABILITY_AUTO_EXPAND");
    assertThat(cloudFormationCreateStackHandler.getCapabilities(
                   AwsConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);

    userDefinedCapabilities = null;
    assertThat(cloudFormationCreateStackHandler.getCapabilities(
                   AwsConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);

    userDefinedCapabilities = Collections.singletonList("CAPABILITY_AUTO_EXPAND");
    expectedCapabilities = Collections.singletonList("CAPABILITY_AUTO_EXPAND");
    doReturn(Collections.emptyList())
        .when(awsCFHelperServiceDelegate)
        .getCapabilities(any(AwsConfig.class), anyString(), anyString(), anyString());
    assertThat(cloudFormationCreateStackHandler.getCapabilities(
                   AwsConfig.builder().build(), "us-east-2", "data", userDefinedCapabilities, "type"))
        .hasSameElementsAs(expectedCapabilities);
  }
}
