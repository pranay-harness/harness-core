/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.rule.OwnerRule.HINGER;
import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.security.PermissionAttribute.PermissionType.MANAGE_APPLICATIONS;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.limits.LimitCheckerFactory;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.beans.Application;
import software.wings.security.annotations.AuthRule;
import software.wings.service.intfc.AppService;
import software.wings.utils.ResourceTestRule;

import java.lang.reflect.Method;
import java.util.ArrayList;
import javax.ws.rs.core.GenericType;
import lombok.extern.slf4j.Slf4j;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

/**
 * The Class AppResourceTest.
 */
@Slf4j
public class AppResourceTest extends CategoryTest {
  private static final AppService appService = mock(AppService.class);
  private static final LimitCheckerFactory limitCheckerFactory = mock(LimitCheckerFactory.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().instance(new AppResource(appService, limitCheckerFactory)).build();
  private final long TIME_IN_MS = System.currentTimeMillis();
  private final String TEST_UUID = "TEST-UUID-" + TIME_IN_MS;
  private final String TEST_UUID2 = "TEST-UUID2-" + TIME_IN_MS + 10;
  private final String TEST_ACCOUNT_ID = "TEST-ACCOUNT-ID-" + TIME_IN_MS;
  private final String TEST_NAME = "TestApp_" + TIME_IN_MS;
  private final String TEST_DESCRIPTION = "stuff";
  private final String TEST_YAML =
      "--- # app.yaml for new Application\nname: " + TEST_NAME + "\ndescription: " + TEST_DESCRIPTION;

  private final Application testApp = anApplication().uuid(TEST_UUID).build();
  private final Application testApp2 = anApplication()
                                           .uuid(TEST_UUID2)
                                           .accountId(TEST_ACCOUNT_ID)
                                           .appId(TEST_UUID2)
                                           .name(TEST_NAME)
                                           .description(TEST_DESCRIPTION)
                                           .build();

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    // we have to reset the mock after each test because of the
    // @ClassRule, or use a @Rule as mentioned below.
    reset(appService);
  }

  /**
   * Test find by name.
   */
  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void testFindByName() {
    when(appService.get(TEST_UUID, true)).thenReturn(testApp);
    RestResponse<Application> actual =
        resources.client().target("/apps/" + TEST_UUID).request().get(new GenericType<RestResponse<Application>>() {});
    assertThat(actual.getResource()).isEqualTo(testApp);
    verify(appService).get(TEST_UUID, true);
  }

  @Test
  @Owner(developers = HINGER)
  @Category(UnitTests.class)
  public void checkIfPermissionCorrect() throws NoSuchMethodException {
    ArrayList<String> methodList = new ArrayList<>();
    methodList.add("save");
    methodList.add("update");

    for (String methodName : methodList) {
      Method method = AppResource.class.getMethod(methodName, String.class, Application.class);
      testSingleMethodPermission(method);
    }

    // Testing delete method has correct permission
    Method deleteMethod = AppResource.class.getMethod("delete", String.class);
    testSingleMethodPermission(deleteMethod);
  }

  private void testSingleMethodPermission(Method method) {
    AuthRule annotation = method.getAnnotation(AuthRule.class);
    assertThat(annotation.permissionType()).isEqualTo(MANAGE_APPLICATIONS);
  }
}
