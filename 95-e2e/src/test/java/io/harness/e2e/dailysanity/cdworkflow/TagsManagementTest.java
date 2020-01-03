package io.harness.e2e.dailysanity.cdworkflow;

import static io.harness.rule.OwnerRule.JUHI;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;

import com.google.inject.Inject;

import io.harness.category.element.E2ETests;
import io.harness.e2e.AbstractE2ETest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.ServiceGenerator;
import io.harness.rule.Owner;
import io.harness.testframework.restutils.ApplicationRestUtils;
import io.harness.testframework.restutils.ServiceRestUtils;
import io.harness.testframework.restutils.TagsManagementUtils;
import io.restassured.path.json.JsonPath;
import lombok.extern.slf4j.Slf4j;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.MethodSorters;
import software.wings.beans.Application;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.Service;
import software.wings.utils.ArtifactType;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TagsManagementTest extends AbstractE2ETest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private ApplicationGenerator applicationGenerator;
  // Test Constants
  private static long APPEND_TEXT = System.currentTimeMillis();
  private static String RESTRICTED_TAG_KEY = "Restricted tag " + APPEND_TEXT;
  private static String RESTRICTED_TAG_UUID = "";

  private static String NORMAL_TAG_KEY = "Normal tag " + APPEND_TEXT;
  private static String NORMAL_TAG_UUID = "";
  private static String APPLICATION_NAME = "Tags Management Test App " + APPEND_TEXT;
  private static String SERVICE_NAME = "Service " + APPEND_TEXT;

  private static String DELETE_TAG_KEY = NORMAL_TAG_KEY + " DELETE";

  // Tag Management Test

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC0_listAllTags() {
    JsonPath tags = TagsManagementUtils.listTags(bearerToken, getAccount().getUuid());
    assertThat(tags).isNotNull();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC1_createRestrictedTag() {
    Set<String> allowedValues = new HashSet<>();
    allowedValues.add("One");
    allowedValues.add("Two");
    HarnessTag tag = HarnessTag.builder().key(RESTRICTED_TAG_KEY).allowedValues(allowedValues).build();
    JsonPath createRestrictedTagResponse = TagsManagementUtils.createTag(bearerToken, getAccount().getUuid(), tag);
    RESTRICTED_TAG_UUID = createRestrictedTagResponse.get("resource.uuid");
    JsonPath getTag = TagsManagementUtils.getTag(bearerToken, getAccount().getUuid(), RESTRICTED_TAG_KEY);
    assertThat(getTag.getList("resource.response").size() == 1).isTrue();
    assertThat(getTag.getList("resource.response[0].allowedValues").size() == 2).isTrue();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC2_createNormalTag() {
    HarnessTag tag = HarnessTag.builder().key(NORMAL_TAG_KEY).build();
    JsonPath createNormalTagResponse = TagsManagementUtils.createTag(bearerToken, getAccount().getUuid(), tag);
    NORMAL_TAG_UUID = createNormalTagResponse.get("resource.uuid");
    JsonPath getTag = TagsManagementUtils.getTag(bearerToken, getAccount().getUuid(), NORMAL_TAG_KEY);
    assertThat(getTag.getList("resource.response").size() == 1).isTrue();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC3_EditTag() {
    Set<String> allowedValues = new HashSet<>();
    allowedValues.add("One");
    allowedValues.add("Two");
    allowedValues.add("Three");

    HarnessTag tag = HarnessTag.builder()
                         .key(RESTRICTED_TAG_KEY)
                         .accountId(getAccount().getUuid())
                         .uuid(RESTRICTED_TAG_UUID)
                         .allowedValues(allowedValues)
                         .build();
    JsonPath createNormalTagResponse =
        TagsManagementUtils.editTag(bearerToken, getAccount().getUuid(), RESTRICTED_TAG_KEY, tag);
    logger.info(createNormalTagResponse.get("resource.key"));
    JsonPath getTag = TagsManagementUtils.getTag(bearerToken, getAccount().getUuid(), RESTRICTED_TAG_KEY);
    assertThat(getTag.getList("resource.response").size() == 1).isTrue();
    assertThat(getTag.getList("resource.response[0].allowedValues").size() == 3).isTrue();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC4_AssociateTag() {
    Application tagsApp = anApplication().name(APPLICATION_NAME).build();
    Application application = ApplicationRestUtils.createApplication(bearerToken, getAccount(), tagsApp);
    Service service = Service.builder().name(SERVICE_NAME).artifactType(ArtifactType.DOCKER).build();
    String serviceId =
        ServiceRestUtils.createService(bearerToken, getAccount().getUuid(), application.getAppId(), service);

    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .entityId(serviceId)
                                 .entityType(EntityType.SERVICE)
                                 .key(RESTRICTED_TAG_KEY)
                                 .value("One")
                                 .build();
    JsonPath attachTagResponse =
        TagsManagementUtils.attachTag(bearerToken, getAccount().getUuid(), application.getAppId(), tagLink);
    assertThat(attachTagResponse).isNotNull();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC5_TagUsageDetails() {
    JsonPath tagsUsageDetails =
        TagsManagementUtils.getTagUsageDetails(bearerToken, getAccount().getUuid(), RESTRICTED_TAG_KEY);
    assertThat(tagsUsageDetails).isNotNull();
    assertThat(tagsUsageDetails.getList("resource.response").size() == 1).isTrue();
  }

  @Test
  @Owner(developers = JUHI)
  @Category(E2ETests.class)
  public void TC6_DeleteTag() {
    HarnessTag tag = HarnessTag.builder().key(DELETE_TAG_KEY).build();
    JsonPath createNormalTagResponse = TagsManagementUtils.createTag(bearerToken, getAccount().getUuid(), tag);
    NORMAL_TAG_UUID = createNormalTagResponse.get("resource.uuid");
    JsonPath getTagBeforeDelete = TagsManagementUtils.getTag(bearerToken, getAccount().getUuid(), DELETE_TAG_KEY);
    assertThat(getTagBeforeDelete.getList("resource.response").size() == 1).isTrue();

    JsonPath deleteTag = TagsManagementUtils.deleteTag(bearerToken, getAccount().getUuid(), DELETE_TAG_KEY);
    JsonPath getTagAfterDelete = TagsManagementUtils.getTag(bearerToken, getAccount().getUuid(), DELETE_TAG_KEY);
    assertThat(getTagAfterDelete.getList("resource.response").size() == 0).isTrue();
  }
}
