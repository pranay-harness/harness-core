/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.trigger;

import static io.harness.rule.OwnerRule.MILAN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;

import software.wings.beans.trigger.Trigger;
import software.wings.graphql.datafetcher.MutationContext;
import software.wings.graphql.schema.type.trigger.QLDeleteTriggerInput;
import software.wings.graphql.schema.type.trigger.QLDeleteTriggerPayload;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.TriggerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

@OwnedBy(HarnessTeam.CDC)
public class DeleteTriggerDataFetcherTest extends CategoryTest {
  @Mock TriggerService triggerService;
  @Mock AppService appService;

  @InjectMocks
  @Spy
  DeleteTriggerDataFetcher deleteTriggerDataFetcher = new DeleteTriggerDataFetcher(triggerService, appService);

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void mutateAndFetchShouldThrowAppMustNotBeEmptyException() {
    MutationContext mutationContext = MutationContext.builder().accountId("mutationContextAccountId").build();
    QLDeleteTriggerInput qlDeleteTriggerInput =
        QLDeleteTriggerInput.builder().triggerId("triggerId").applicationId("").clientMutationId("mutationId").build();

    deleteTriggerDataFetcher.mutateAndFetch(qlDeleteTriggerInput, mutationContext);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void mutateAndFetchShouldThrowAppDoesntBelongToAccountException() {
    MutationContext mutationContext = MutationContext.builder().accountId("mutationContextAccountId").build();
    QLDeleteTriggerInput qlDeleteTriggerInput = QLDeleteTriggerInput.builder()
                                                    .triggerId("triggerId")
                                                    .applicationId("appId")
                                                    .clientMutationId("mutationId")
                                                    .build();

    when(appService.getAccountIdByAppId(qlDeleteTriggerInput.getApplicationId())).thenReturn("accountId");

    deleteTriggerDataFetcher.mutateAndFetch(qlDeleteTriggerInput, mutationContext);
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void mutateAndFetchShouldNotDeleteTrigger() {
    MutationContext mutationContext = MutationContext.builder().accountId("accountId").build();
    QLDeleteTriggerInput qlDeleteTriggerInput = QLDeleteTriggerInput.builder()
                                                    .triggerId("triggerId")
                                                    .applicationId("appId")
                                                    .clientMutationId("mutationId")
                                                    .build();

    when(appService.getAccountIdByAppId(qlDeleteTriggerInput.getApplicationId())).thenReturn("accountId");
    when(triggerService.get(qlDeleteTriggerInput.getApplicationId(), qlDeleteTriggerInput.getTriggerId()))
        .thenReturn(Trigger.builder().build());

    QLDeleteTriggerPayload qlDeleteTriggerPayload =
        deleteTriggerDataFetcher.mutateAndFetch(qlDeleteTriggerInput, mutationContext);
  }

  @Test()
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void mutateAndFetchShouldDeleteTrigger() {
    MutationContext mutationContext = MutationContext.builder().accountId("accountId").build();
    QLDeleteTriggerInput qlDeleteTriggerInput = QLDeleteTriggerInput.builder()
                                                    .triggerId("triggerId")
                                                    .applicationId("appId")
                                                    .clientMutationId("mutationId")
                                                    .build();

    Trigger trigger = Mockito.mock(Trigger.class);

    when(appService.getAccountIdByAppId(qlDeleteTriggerInput.getApplicationId())).thenReturn("accountId");
    when(triggerService.get(qlDeleteTriggerInput.getApplicationId(), qlDeleteTriggerInput.getTriggerId()))
        .thenReturn(trigger, null);
    when(triggerService.triggerActionExists(trigger)).thenReturn(true);
    Mockito.doNothing().when(triggerService).authorize(trigger, true);
    when(triggerService.delete(qlDeleteTriggerInput.getApplicationId(), qlDeleteTriggerInput.getTriggerId()))
        .thenReturn(true);

    QLDeleteTriggerPayload qlDeleteTriggerPayload =
        deleteTriggerDataFetcher.mutateAndFetch(qlDeleteTriggerInput, mutationContext);

    Mockito.verify(triggerService, Mockito.times(2)).get(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(triggerService, Mockito.times(1)).triggerActionExists(Matchers.any(Trigger.class));
    Mockito.verify(triggerService, Mockito.times(1)).authorize(Matchers.any(Trigger.class), Matchers.anyBoolean());
    Mockito.verify(triggerService, Mockito.times(1)).delete(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(appService, Mockito.times(1)).getAccountIdByAppId(Matchers.anyString());

    assertThat(qlDeleteTriggerPayload).isNotNull();
    assertThat(qlDeleteTriggerPayload.getClientMutationId()).isEqualTo(qlDeleteTriggerInput.getClientMutationId());
  }

  @Test(expected = InvalidRequestException.class)
  @Owner(developers = MILAN)
  @Category(UnitTests.class)
  public void mutateAndFetchShouldThrowTriggerIsNotDeletedException() {
    MutationContext mutationContext = MutationContext.builder().accountId("accountId").build();
    QLDeleteTriggerInput qlDeleteTriggerInput = QLDeleteTriggerInput.builder()
                                                    .triggerId("triggerId")
                                                    .applicationId("appId")
                                                    .clientMutationId("mutationId")
                                                    .build();

    Trigger trigger = Mockito.mock(Trigger.class);

    when(appService.getAccountIdByAppId(qlDeleteTriggerInput.getApplicationId())).thenReturn("accountId");
    when(triggerService.get(qlDeleteTriggerInput.getApplicationId(), qlDeleteTriggerInput.getTriggerId()))
        .thenReturn(trigger, trigger);
    when(triggerService.triggerActionExists(trigger)).thenReturn(true);
    Mockito.doNothing().when(triggerService).authorize(trigger, true);
    when(triggerService.delete(qlDeleteTriggerInput.getApplicationId(), qlDeleteTriggerInput.getTriggerId()))
        .thenReturn(true);

    deleteTriggerDataFetcher.mutateAndFetch(qlDeleteTriggerInput, mutationContext);

    Mockito.verify(triggerService, Mockito.times(2)).get(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(triggerService, Mockito.times(1)).triggerActionExists(Matchers.any(Trigger.class));
    Mockito.verify(triggerService, Mockito.times(1)).authorize(Matchers.any(Trigger.class), Matchers.anyBoolean());
    Mockito.verify(triggerService, Mockito.times(1)).delete(Matchers.anyString(), Matchers.anyString());
    Mockito.verify(appService, Mockito.times(1)).getAccountIdByAppId(Matchers.anyString());
  }
}
