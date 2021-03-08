package io.harness.ngtriggers.eventmapper.filters.impl;

import static io.harness.rule.OwnerRule.ADWAIT;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ngtriggers.beans.dto.TriggerDetails;
import io.harness.ngtriggers.beans.dto.eventmapping.WebhookEventMappingResponse;
import io.harness.ngtriggers.beans.entity.NGTriggerEntity;
import io.harness.ngtriggers.beans.entity.TriggerWebhookEvent;
import io.harness.ngtriggers.beans.response.WebhookEventResponse.FinalStatus;
import io.harness.ngtriggers.beans.scm.WebhookPayloadData;
import io.harness.ngtriggers.eventmapper.filters.dto.FilterRequestData;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class TriggerProjectFilterTest extends CategoryTest {
  @Mock private NGTriggerService ngTriggerService;
  @Inject @InjectMocks private ProjectTriggerFilter filter;
  private static List<NGTriggerEntity> triggerEntities;

  @Before
  public void setUp() throws IOException {
    initMocks(this);
  }

  @Test
  @Owner(developers = ADWAIT)
  @Category(UnitTests.class)
  public void applyRepoUrlFilterTest() {
    NGTriggerEntity t1 = NGTriggerEntity.builder().identifier("T1").build();
    NGTriggerEntity t2 = NGTriggerEntity.builder().identifier("T2").build();

    doReturn(null)
        .doReturn(Arrays.asList(t1, t2))
        .when(ngTriggerService)
        .listEnabledTriggersForCurrentProject("acc", "org", "proj");

    FilterRequestData filterRequestData = FilterRequestData.builder()
                                              .projectFqn("acc/org/proj")
                                              .webhookPayloadData(WebhookPayloadData.builder()
                                                                      .originalEvent(TriggerWebhookEvent.builder()
                                                                                         .accountId("acc")
                                                                                         .orgIdentifier("org")
                                                                                         .projectIdentifier("proj")
                                                                                         .sourceRepoType("GITHUB")
                                                                                         .createdAt(0l)
                                                                                         .nextIteration(0l)
                                                                                         .build())
                                                                      .build())
                                              .build();

    WebhookEventMappingResponse webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isTrue();
    assertThat(webhookEventMappingResponse.getWebhookEventResponse().getFinalStatus())
        .isEqualTo(FinalStatus.NO_ENABLED_TRIGGER_FOR_PROJECT);

    webhookEventMappingResponse = filter.applyFilter(filterRequestData);
    assertThat(webhookEventMappingResponse.isFailedToFindTrigger()).isFalse();
    List<TriggerDetails> triggerDetails = webhookEventMappingResponse.getTriggers();
    assertThat(triggerDetails.size()).isEqualTo(2);
    List<NGTriggerEntity> entities =
        triggerDetails.stream().map(triggerDetails1 -> triggerDetails1.getNgTriggerEntity()).collect(toList());
    assertThat(entities).containsExactlyInAnyOrder(t1, t2);
  }
}
