/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CEMailUnsubscribeResourceTest extends CategoryTest {
  private String accountId = "ACCOUNT_ID";
  private String uuid = "UUID";
  private String email = "user@harness.io";
  private CECommunications communications;
  private CommunicationType type = CommunicationType.WEEKLY_REPORT;

  private static CECommunicationsService communicationsService = mock(CECommunicationsService.class);
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().instance(new CEMailUnsubscribeResource(communicationsService)).build();

  @Before
  public void setUp() {
    communications = CECommunications.builder().uuid(uuid).accountId(accountId).emailId(email).build();
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testUpdate() {
    assertThatThrownBy(()
                           -> RESOURCES.client()
                                  .target(format("/ceMailUnsubscribe/%s", uuid))
                                  .request()
                                  .post(entity(communications, MediaType.APPLICATION_JSON),
                                      new GenericType<RestResponse<CECommunications>>() {}));
    verify(communicationsService).unsubscribe(uuid);
  }
}
