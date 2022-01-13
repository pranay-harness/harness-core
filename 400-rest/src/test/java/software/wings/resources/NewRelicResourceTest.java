/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.impl.newrelic.NewRelicApplication;
import software.wings.service.intfc.newrelic.NewRelicService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.GenericType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;

public class NewRelicResourceTest extends WingsBaseTest {
  private static final NewRelicResource NEW_RELIC_RESOURCE = new NewRelicResource();
  @Mock private NewRelicService newRelicService;

  private static final String baseUrl = "/newrelic";
  private String accountId;
  private String settingId;
  private String newRelicAppName;
  private long newRelicAppId = 4L;

  private NewRelicApplication newRelicApplication;

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().instance(NEW_RELIC_RESOURCE).type(WingsExceptionMapper.class).build();

  @Before
  public void setUp() throws IllegalAccessException {
    accountId = generateUuid();
    settingId = generateUuid();
    newRelicAppName = generateUuid();

    FieldUtils.writeField(NEW_RELIC_RESOURCE, "newRelicService", newRelicService, true);

    newRelicApplication = NewRelicApplication.builder().name(newRelicAppName).id(newRelicAppId).build();
    when(newRelicService.resolveApplicationName(settingId, newRelicAppName)).thenReturn(newRelicApplication);
    when(newRelicService.resolveApplicationId(settingId, String.valueOf(newRelicAppId)))
        .thenReturn(newRelicApplication);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveNewRelicAppName_correctApiSignature() {
    RestResponse<NewRelicApplication> restResponse =
        RESOURCES.client()
            .target(baseUrl + "/resolve-application-name?accountId=" + accountId + "&settingId=" + settingId
                + "&newRelicApplicationName=" + newRelicAppName)
            .request()
            .get(new GenericType<RestResponse<NewRelicApplication>>() {});
    assertThat(restResponse.getResource()).isEqualTo(newRelicApplication);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveNewRelicAppName_incorrectApiSignature() {
    assertThatThrownBy(()
                           -> RESOURCES.client()
                                  .target(baseUrl + "/resolve-application-name?accountId=" + accountId
                                      + "&newRelicApplicationName=" + newRelicAppName)
                                  .request()
                                  .get(new GenericType<RestResponse<NewRelicApplication>>() {}))
        .isInstanceOf(BadRequestException.class);

    assertThatThrownBy(
        ()
            -> RESOURCES.client()
                   .target(baseUrl + "/resolve-application-name?accountId=" + accountId + "&settingId=" + settingId)
                   .request()
                   .get(new GenericType<RestResponse<NewRelicApplication>>() {}))
        .isInstanceOf(BadRequestException.class);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveNewRelicAppId_correctApiSignature() {
    RestResponse<NewRelicApplication> restResponse =
        RESOURCES.client()
            .target(baseUrl + "/resolve-application-id?accountId=" + accountId + "&settingId=" + settingId
                + "&newRelicApplicationId=" + newRelicAppId)
            .request()
            .get(new GenericType<RestResponse<NewRelicApplication>>() {});
    assertThat(restResponse.getResource()).isEqualTo(newRelicApplication);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveNewRelicAppId_incorrectApiSignature() {
    assertThatThrownBy(()
                           -> RESOURCES.client()
                                  .target(baseUrl + "/resolve-application-id?accountId=" + accountId
                                      + "&newRelicApplicationId=" + newRelicAppId)
                                  .request()
                                  .get(new GenericType<RestResponse<NewRelicApplication>>() {}))
        .isInstanceOf(BadRequestException.class);

    assertThatThrownBy(
        ()
            -> RESOURCES.client()
                   .target(baseUrl + "/resolve-application-id?accountId=" + accountId + "&settingId=" + settingId)
                   .request()
                   .get(new GenericType<RestResponse<NewRelicApplication>>() {}))
        .isInstanceOf(BadRequestException.class);
  }
}
