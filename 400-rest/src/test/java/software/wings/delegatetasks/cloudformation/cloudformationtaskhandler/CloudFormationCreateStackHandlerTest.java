/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks.cloudformation.cloudformationtaskhandler;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.ANIL;
import static io.harness.rule.OwnerRule.PRAKHAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.helpers.ext.cloudformation.request.CloudFormationCreateStackRequest;
import software.wings.service.impl.aws.delegate.AwsCFHelperServiceDelegateImpl;
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
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
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

  @Test
  @Owner(developers = ANIL)
  @Category(UnitTests.class)
  public void testS3TemplatePath() {
    AwsCFHelperServiceDelegate awsCFHelperServiceDelegate = new AwsCFHelperServiceDelegateImpl();
    String s3Path = "https://anil-harness-test.s3.amazonaws.com/anilTest/basicCf.yaml";
    String s3PathWithPlus = "https://anil-harness-test.s3.amazonaws.com/anil%2Btest/basicCf.yaml";
    String s3PathWithSpace = "https://anil-harness-test.s3.amazonaws.com/anil+test/basicCf.yaml";
    String s3PathWithMultipleSpace = "https://anil-harness-test.s3.amazonaws.com/anil+multiple+space+test/basicCf.yaml";
    String s3SpaceInFileName = "https://anil-harness-test.s3.amazonaws.com/anilSpaceInFileName/basic+test.yaml";
    String s3PlusInFileName = "https://anil-harness-test.s3.amazonaws.com/anilPlusInFileName/basic%2Btest.yaml";
    String s3SpaceInFolderAndFileName =
        "https://anil-harness-test.s3.amazonaws.com/anil+test/folder/folder%2Btest/basic+test.yaml";

    CloudFormationCreateStackRequest request = CloudFormationCreateStackRequest.builder().data(s3Path).build();
    String normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath).isEqualTo(s3Path);

    request.setData(s3PathWithPlus);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath).isEqualTo(s3PathWithPlus);

    request.setData(s3PathWithSpace);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath).isEqualTo("https://anil-harness-test.s3.amazonaws.com/anil%20test/basicCf.yaml");

    request.setData(s3PathWithMultipleSpace);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath)
        .isEqualTo("https://anil-harness-test.s3.amazonaws.com/anil%20multiple%20space%20test/basicCf.yaml");

    request.setData(s3SpaceInFileName);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath)
        .isEqualTo("https://anil-harness-test.s3.amazonaws.com/anilSpaceInFileName/basic%20test.yaml");

    request.setData(s3PlusInFileName);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath)
        .isEqualTo("https://anil-harness-test.s3.amazonaws.com/anilPlusInFileName/basic%2Btest.yaml");

    request.setData(s3SpaceInFolderAndFileName);
    normalizedPath = awsCFHelperServiceDelegate.normalizeS3TemplatePath(request.getData());
    assertThat(normalizedPath)
        .isEqualTo("https://anil-harness-test.s3.amazonaws.com/anil%20test/folder/folder%2Btest/basic%20test.yaml");
  }
}
