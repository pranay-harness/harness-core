package software.wings.service;

import static io.harness.beans.SearchFilter.Operator.EQ;
import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.EntityType.SERVICE;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.HarnessTag;
import software.wings.beans.HarnessTagLink;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.HarnessTagServiceImpl;

import java.util.HashSet;

public class HarnessTagServiceTest extends WingsBaseTest {
  private static final String TEST_ACCOUNT_ID = "TEST_ACCOUNT_ID";

  @Mock private MainConfiguration mainConfiguration;
  @Inject @InjectMocks @Spy private HarnessTagServiceImpl harnessTagService;

  @Inject private WingsPersistence wingsPersistence;

  private String colorTagKey = "color";
  private HarnessTag colorTag = HarnessTag.builder().accountId(TEST_ACCOUNT_ID).key(colorTagKey).build();

  @Before
  public void setUp() throws Exception {}

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
      assertThat(exception.getParams().get("message")).isEqualTo("Tag key cannot be blank");
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
      assertThat(exception.getParams().get("message")).isEqualTo("Max allowed size for tag key is 128");
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
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(request);

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
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(requestColorRed);
    assertThat(resources).isNotNull();
    assertThat(resources.getResponse()).hasSize(1);
    assertThat(resources.getResponse().get(0).getKey()).isEqualTo(colorTagKey);
    assertThat(resources.getResponse().get(0).getValue()).isEqualTo("red");
    assertThat(resources.getResponse().get(0).getEntityType()).isEqualTo(SERVICE);
    assertThat(resources.getResponse().get(0).getEntityId()).isEqualTo("id");

    tagLink.setValue("blue");
    harnessTagService.attachTag(tagLink);

    resources = harnessTagService.listResourcesWithTag(requestColorRed);
    assertThat(resources.getResponse()).isEmpty();

    PageRequest<HarnessTagLink> requestColorBlue = new PageRequest<>();
    requestColorBlue.addFilter("accountId", EQ, TEST_ACCOUNT_ID);
    requestColorBlue.addFilter("key", EQ, colorTagKey);
    requestColorRed.addFilter("value", EQ, "blue");
    resources = harnessTagService.listResourcesWithTag(requestColorBlue);

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
    PageResponse<HarnessTagLink> resources = harnessTagService.listResourcesWithTag(request);

    try {
      harnessTagService.delete(colorTag);
      fail("Expected an InvalidRequestException to be thrown");
    } catch (InvalidRequestException exception) {
      assertThat(exception.getParams().get("message")).isEqualTo("Tag is in use. Cannot delete");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void getInUseValuesTest() {
    HarnessTagLink tagLinkRed = HarnessTagLink.builder()
                                    .accountId(TEST_ACCOUNT_ID)
                                    .entityId("id1")
                                    .entityType(SERVICE)
                                    .key(colorTagKey)
                                    .value("red")
                                    .build();

    harnessTagService.attachTag(tagLinkRed);

    HarnessTagLink tagLinkBlue = HarnessTagLink.builder()
                                     .accountId(TEST_ACCOUNT_ID)
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
}
