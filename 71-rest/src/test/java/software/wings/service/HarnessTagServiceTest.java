package software.wings.service;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.RealMongo;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.EntityType;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.beans.ResourceLookup;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.EntityNameCache;
import software.wings.service.impl.HarnessTagServiceImpl;
import software.wings.service.intfc.ResourceLookupService;

import java.util.Collections;
import java.util.HashSet;

public class HarnessTagServiceTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";

  @Mock private MainConfiguration mainConfiguration;
  @Mock private ResourceLookupService resourceLookupService;
  @Mock EntityNameCache entityNameCache;

  @Inject @InjectMocks @Spy private HarnessTagServiceImpl harnessTagService;

  @Inject private WingsPersistence wingsPersistence;

  private String colorTagKey = "color";
  private HarnessTag colorTag = HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key(colorTagKey).build();

  @Before
  public void setUp() throws Exception {
    when(resourceLookupService.getWithResourceId(TEST_ACCOUNT_ID, "id")).thenReturn(getResourceLookupWithId("id"));
  }

  @Test
  @Category(UnitTests.class)
  public void smokeTest() {
    harnessTagService.create(colorTag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag).isNotNull();
    assertThat(savedTag.getUuid()).isNotEmpty();
    assertThat(savedTag).hasFieldOrPropertyWithValue("key", colorTagKey);
  }

  @Test
  @Category(UnitTests.class)
  public void invalidKeyTest() {
    try {
      harnessTagService.create(HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key(" ").build());
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag name cannot be blank");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void invalidKeyLengthTest() {
    try {
      harnessTagService.create(
          HarnessTag.builder()
              .accountId(TEST_ACCOUNT_ID)
              .key(
                  "aaaaaaaaaaaaaaaaadasdsda12342453sadfasaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaasdfasfasfaaaaaaaaaaaaaaaaa")
              .build());
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Max allowed size for tag name is 128");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void invalidAllowedValueTest() {
    try {
      HashSet<String> allowedValues = new HashSet<>();
      allowedValues.add(null);
      harnessTagService.create(
          HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key("test").allowedValues(allowedValues).build());
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag value cannot be null");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void invalidAllowedValuesLengthTest() {
    try {
      HashSet<String> allowedValues = new HashSet<>();
      allowedValues.add(
          "aaaaaaaaaaaaaaaaadasdsda12342453sadfasaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaasdfasfasfaaaaaaaaaaaaaaaaa"
          + "aaaaaaaaaaaaaaaaadasdsda12342453sadfasaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaasdfasfasfaaaaaaaaaaaaaaaaa");

      harnessTagService.create(
          HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key("test").allowedValues(allowedValues).build());
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Max allowed size for tag value is 256");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void updateTest() {
    harnessTagService.create(colorTag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    HashSet<String> allowedValues = new HashSet<>();
    allowedValues.add("red");
    allowedValues.add("green");
    allowedValues.add("blue");
    savedTag.setAllowedValues(allowedValues);
    harnessTagService.update(savedTag);
    HarnessTag savedTag1 = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag1.getAllowedValues()).isEqualTo(allowedValues);
  }

  @Test
  @Category(UnitTests.class)
  public void deleteTagTest() {
    harnessTagService.create(colorTag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag).isNotNull();
    harnessTagService.delete(colorTag);
    HarnessTag savedTag1 = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag1).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void attachTagSmokeTest() {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .entityId("id")
                                    .entityType(SERVICE)
                                    .key(colorTagKey)
                                    .value("red")
                                    .build());
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag).isNotNull();
    PageRequest<HarnessTagLink> request = new PageRequest<>();
    request.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    request.addFilter("key", EQ, colorTagKey);
    request.addFilter("value", EQ, "red");
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, request);

    assertThat(resources).isNotNull();
    assertThat(resources.getResponse()).hasSize(1);
    assertThat(resources.getResponse().get(0).getKey()).isEqualTo(colorTagKey);
    assertThat(resources.getResponse().get(0).getValue()).isEqualTo("red");
    assertThat(resources.getResponse().get(0).getEntityType()).isEqualTo(SERVICE);
    assertThat(resources.getResponse().get(0).getEntityId()).isEqualTo("id");
  }

  @Test
  @Category(UnitTests.class)
  public void updateTagValueTest() {
    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .accountId(TEST_ACCOUNT_ID)
                                 .appId(APP_ID)
                                 .entityId("id")
                                 .entityType(SERVICE)
                                 .key(colorTagKey)
                                 .value("red")
                                 .build();

    harnessTagService.attachTag(tagLink);

    PageRequest<HarnessTagLink> requestColorRed = new PageRequest<>();
    requestColorRed.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    requestColorRed.addFilter("key", EQ, colorTagKey);
    requestColorRed.addFilter("value", EQ, "red");
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, requestColorRed);
    assertThat(resources).isNotNull();
    assertThat(resources.getResponse()).hasSize(1);
    assertThat(resources.getResponse().get(0).getKey()).isEqualTo(colorTagKey);
    assertThat(resources.getResponse().get(0).getValue()).isEqualTo("red");
    assertThat(resources.getResponse().get(0).getEntityType()).isEqualTo(SERVICE);
    assertThat(resources.getResponse().get(0).getEntityId()).isEqualTo("id");

    tagLink.setValue("blue");
    harnessTagService.attachTag(tagLink);

    resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, requestColorRed);
    assertThat(resources.getResponse()).isEmpty();

    PageRequest<HarnessTagLink> requestColorBlue = new PageRequest<>();
    requestColorBlue.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    requestColorBlue.addFilter("key", EQ, colorTagKey);
    requestColorRed.addFilter("value", EQ, "blue");
    resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, requestColorBlue);

    assertThat(resources).isNotNull();
    assertThat(resources.getResponse()).hasSize(1);
    assertThat(resources.getResponse().get(0).getKey()).isEqualTo(colorTagKey);
    assertThat(resources.getResponse().get(0).getValue()).isEqualTo("blue");
    assertThat(resources.getResponse().get(0).getEntityType()).isEqualTo(SERVICE);
    assertThat(resources.getResponse().get(0).getEntityId()).isEqualTo("id");
  }

  @Test
  @Category(UnitTests.class)
  public void tryToDeleteInUseTagTest() {
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .entityId("id")
                                    .entityType(SERVICE)
                                    .key(colorTagKey)
                                    .value("red")
                                    .build());
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag).isNotNull();
    PageRequest<HarnessTagLink> request = new PageRequest<>();
    request.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    request.addFilter("key", EQ, colorTagKey);
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, request);

    try {
      harnessTagService.delete(colorTag);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag is in use. Cannot delete");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void tryToDeleteInUseAllowedValueTest() {
    colorTag.setAllowedValues(Sets.newHashSet("red"));
    harnessTagService.create(colorTag);
    harnessTagService.attachTag(HarnessTagLink.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .entityId("id")
                                    .entityType(SERVICE)
                                    .key(colorTagKey)
                                    .value("red")
                                    .build());
    colorTag.setAllowedValues(Collections.emptySet());

    try {
      harnessTagService.update(colorTag);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag value red is in use. Cannot delete");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getInUseValuesTest() {
    when(resourceLookupService.getWithResourceId(TEST_ACCOUNT_ID, "id1")).thenReturn(getResourceLookupWithId("id1"));
    when(resourceLookupService.getWithResourceId(TEST_ACCOUNT_ID, "id2")).thenReturn(getResourceLookupWithId("id2"));

    HarnessTagLink tagLinkRed = HarnessTagLink.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .appId(APP_ID)
                                    .entityId("id1")
                                    .entityType(SERVICE)
                                    .key(colorTagKey)
                                    .value("red")
                                    .build();

    harnessTagService.attachTag(tagLinkRed);

    HarnessTagLink tagLinkBlue = HarnessTagLink.builder()
                                     .accountId(TEST_ACCOUNT_ID)
                                     .appId(APP_ID)
                                     .entityId("id2")
                                     .entityType(SERVICE)
                                     .key(colorTagKey)
                                     .value("blue")
                                     .build();

    harnessTagService.attachTag(tagLinkBlue);

    HarnessTag tag = harnessTagService.getTagWithInUseValues(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(tag.getKey()).isEqualTo(colorTagKey);
    assertThat(tag.getInUseValues()).contains("red");
    assertThat(tag.getInUseValues()).containsAll(ImmutableSet.of("red", "blue"));
  }

  private ResourceLookup getResourceLookupWithId(String resourceId) {
    return ResourceLookup.builder()
        .appId(APP_ID)
        .resourceType(EntityType.SERVICE.name())
        .resourceId(resourceId)
        .build();
  }

  @Test
  @Category(UnitTests.class)
  public void testAttachTagWithEmptyValue() {
    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .accountId(TEST_ACCOUNT_ID)
                                 .appId(APP_ID)
                                 .entityId("id")
                                 .entityType(SERVICE)
                                 .key(colorTagKey)
                                 .value("")
                                 .build();

    harnessTagService.attachTag(tagLink);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag).isNotNull();
    PageRequest<HarnessTagLink> request = new PageRequest<>();
    request.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    request.addFilter("key", EQ, colorTagKey);
    request.addFilter("value", EQ, "");
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(TEST_ACCOUNT_ID, request);

    assertThat(resources).isNotNull();
    assertThat(resources.getResponse()).hasSize(1);
    assertThat(resources.getResponse().get(0).getKey()).isEqualTo(colorTagKey);
    assertThat(resources.getResponse().get(0).getValue()).isEqualTo("");
    assertThat(resources.getResponse().get(0).getEntityType()).isEqualTo(SERVICE);
    assertThat(resources.getResponse().get(0).getEntityId()).isEqualTo("id");
  }

  @Test
  @Category(UnitTests.class)
  public void testAttachTagWithNullValue() {
    HarnessTagLink tagLink = HarnessTagLink.builder()
                                 .accountId(TEST_ACCOUNT_ID)
                                 .appId(APP_ID)
                                 .entityId("id")
                                 .entityType(SERVICE)
                                 .key(colorTagKey)
                                 .build();

    try {
      harnessTagService.attachTag(tagLink);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag value cannot be null");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void testInvalidTagKey() {
    testInvalidTagKeyUtil("", "Tag name cannot be blank");
    testInvalidTagKeyUtil("  ", "Tag name cannot be blank");
    testInvalidTagKeyUtil(" _key", "Tag name/value cannot begin with .-_/");
    testInvalidTagKeyUtil(" -key", "Tag name/value cannot begin with .-_/");
    testInvalidTagKeyUtil(" .key", "Tag name/value cannot begin with .-_/");
    testInvalidTagKeyUtil(" /key", "Tag name/value cannot begin with .-_/");
    testInvalidTagKeyUtil(" tag + key",
        "Tag name/value can contain only abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_ /");
    testInvalidTagKeyUtil("harness.io/abc", "Unauthorized: harness.io is a reserved Tag name prefix");
  }

  @Test
  @Category(UnitTests.class)
  public void testValidTagKey() {
    testValidTagKeyUtil(" tagKey", "tagKey");
    testValidTagKeyUtil(" tag Key", "tag Key");
    testValidTagKeyUtil(" tag 9 Key ", "tag 9 Key");
    testValidTagKeyUtil(" tag 9 / Key ", "tag 9 / Key");
    testValidTagKeyUtil(" tag 9 _ Key ", "tag 9 _ Key");
    testValidTagKeyUtil(" tag 9 . Key ", "tag 9 . Key");
    testValidTagKeyUtil(" tag 9 Key - ", "tag 9 Key -");
  }

  @Test
  @Category(UnitTests.class)
  public void testInvalidTagValue() {
    testInvalidTagValueUtil(null, "Tag value cannot be null");
    testInvalidTagValueUtil(" _value", "Tag name/value cannot begin with .-_/");
    testInvalidTagValueUtil(" -value", "Tag name/value cannot begin with .-_/");
    testInvalidTagValueUtil(" .value", "Tag name/value cannot begin with .-_/");
    testInvalidTagValueUtil(" /value", "Tag name/value cannot begin with .-_/");
    testInvalidTagValueUtil(" tag + key",
        "Tag name/value can contain only abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789.-_ /");
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateSystemTag() {
    HarnessTag tag = HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key("system/key").build();
    harnessTagService.create(tag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, "system/key");
    assertThat(savedTag).isNotNull();
    assertThat(savedTag.getUuid()).isNotEmpty();
    assertThat(savedTag).hasFieldOrPropertyWithValue("key", "system/key");

    try {
      tag = HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key("system/key1").build();
      harnessTagService.createTag(tag, false, false);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message"))
          .isEqualTo("Unauthorized: User need to have TAG_MANAGEMENT permission to create system tags");
    }
  }

  private void testInvalidTagKeyUtil(String key, String expectedExceptionMessage) {
    try {
      HarnessTag tag = HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key(key).build();
      harnessTagService.create(tag);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo(expectedExceptionMessage);
    }
  }

  private void testValidTagKeyUtil(String key, String expectedKey) {
    HarnessTag tag =
        HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key(key).allowedValues(Sets.newHashSet("")).build();
    harnessTagService.create(tag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, expectedKey);
    assertThat(savedTag).isNotNull();
    assertThat(savedTag.getUuid()).isNotEmpty();
    assertThat(savedTag).hasFieldOrPropertyWithValue("key", expectedKey);
  }

  private void testInvalidTagValueUtil(String value, String expectedExceptionMessage) {
    try {
      HarnessTag tag =
          HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key("key").allowedValues(Sets.newHashSet(value)).build();
      harnessTagService.create(tag);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo(expectedExceptionMessage);
    }
  }

  @Test
  @RealMongo
  @Category(UnitTests.class)
  public void testUpdateTagAllowedValues() {
    harnessTagService.create(colorTag);
    HarnessTag savedTag = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag.getAllowedValues()).isEqualTo(null);

    savedTag.setAllowedValues(Sets.newHashSet("red"));
    harnessTagService.update(savedTag);
    HarnessTag savedTag1 = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag1.getAllowedValues()).isEqualTo(Sets.newHashSet("red"));

    savedTag.setAllowedValues(null);
    harnessTagService.update(savedTag);
    HarnessTag savedTag2 = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag2.getAllowedValues()).isEqualTo(null);

    savedTag.setAllowedValues(Sets.newHashSet("red", "blue", "green"));
    harnessTagService.update(savedTag);
    HarnessTag savedTag3 = harnessTagService.get(TEST_ACCOUNT_ID, colorTagKey);
    assertThat(savedTag3.getAllowedValues()).isEqualTo(Sets.newHashSet("red", "blue", "green"));
  }
}
