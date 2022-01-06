/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.communication.CECommunicationsService;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.utils.ResourceTestRule;

import java.util.Collections;
import java.util.List;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CECommunicationsResourceTest extends CategoryTest {
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().instance(new CECommunicationsResource(null)).build();

  private String accountId = "ACCOUNT_ID";
  private String accountId2 = "ACCOUNT_ID2";
  private String uuid = "UUID";
  private boolean enable = true;
  private String email = "user@harness.io";
  private CECommunications communications;
  private CommunicationType type = CommunicationType.WEEKLY_REPORT;

  private CECommunicationsService communicationsService = mock(CECommunicationsService.class);

  @Before
  public void setUp() throws IllegalAccessException {
    CECommunicationsResource instance = (CECommunicationsResource) RESOURCES.getInstances().iterator().next();
    FieldUtils.writeField(instance, "communicationsService", communicationsService, true);
    communications = CECommunications.builder().uuid(uuid).accountId(accountId).emailId(email).build();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGet() {
    assertThatThrownBy(()
                           -> RESOURCES.client()
                                  .target(format("/ceCommunications?accountId=%s", accountId))
                                  .request()
                                  .get(new GenericType<RestResponse<List<CECommunications>>>() {}))
        .isInstanceOf(ProcessingException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdate() {
    assertThatThrownBy(()
                           -> RESOURCES.client()
                                  .target(format("/ceCommunications?accountId=%s&enable=%s", accountId, type, enable))
                                  .request()
                                  .post(entity(communications, MediaType.APPLICATION_JSON),
                                      new GenericType<RestResponse<CECommunications>>() {}))
        .isInstanceOf(ProcessingException.class);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGetEnabledEntriesViaEmail() {
    RESOURCES.client()
        .target(format("/ceCommunications/%s", accountId))
        .request()
        .get(new GenericType<RestResponse<List<CECommunications>>>() {});
    verify(communicationsService).getEntriesEnabledViaEmail(accountId);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testAddEmail() {
    RESOURCES.client()
        .target(format("/ceCommunications/%s?type=%s&email=%s&enable=%s", accountId, type, email, true))
        .request()
        .post(entity(communications, MediaType.APPLICATION_JSON), new GenericType<RestResponse<CECommunications>>() {});
    verify(communicationsService).update(accountId, email, type, true, false);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testRemoveEmail() {
    RESOURCES.client()
        .target(format("/ceCommunications/%s?type=%s&email=%s", accountId, type, email))
        .request()
        .delete(new GenericType<RestResponse<CECommunications>>() {});
    verify(communicationsService).delete(accountId, email, type);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testAddMultipleEmails() {
    RESOURCES.client()
        .target(format("/ceCommunications/%s/addEmails?type=%s", accountId, type))
        .request()
        .post(entity(Collections.singletonList(email), MediaType.APPLICATION_JSON),
            new GenericType<RestResponse<CECommunications>>() {});
    verify(communicationsService).update(accountId, email, type, true, false);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testAddEmailInternal() {
    RESOURCES.client()
        .target(format(
            "/ceCommunications/%s/internal?type=%s&email=%s&targetAccount=%s", accountId, type, email, accountId2))
        .request()
        .post(entity(communications, MediaType.APPLICATION_JSON), new GenericType<RestResponse<CECommunications>>() {});
    verify(communicationsService).update(accountId2, email, type, true, true);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testRemoveEmailInternal() {
    RESOURCES.client()
        .target(format(
            "/ceCommunications/%s/internal?type=%s&email=%s&targetAccount=%s", accountId, type, email, accountId2))
        .request()
        .delete(new GenericType<RestResponse<CECommunications>>() {});
    verify(communicationsService).delete(accountId2, email, type);
  }
}
