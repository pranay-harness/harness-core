package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.apache.commons.lang3.RandomStringUtils.randomAlphabetic;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.BuilderFactory.Context;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.PagerDutyActivity;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.core.beans.PagerDutyIncidentDTO;
import io.harness.cvng.core.entities.PagerDutyWebhook;
import io.harness.cvng.core.entities.Webhook;
import io.harness.cvng.core.entities.changeSource.PagerDutyChangeSource;
import io.harness.cvng.core.services.api.WebhookService;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WebhookServiceImplTest extends CvNextGenTestBase {
  @Inject private WebhookService webhookService;
  @Inject private HPersistence hPersistence;

  private BuilderFactory builderFactory;
  private Context context;
  private String token;
  private String webhookId;
  private String changeSourceId;

  @Before
  public void setup() {
    builderFactory = BuilderFactory.getDefault();
    context = builderFactory.getContext();

    token = randomAlphabetic(20);
    webhookId = randomAlphabetic(20);
    changeSourceId = randomAlphabetic(20);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_createPagerdutyWebhook() {
    webhookService.createPagerdutyWebhook(context.getServiceEnvironmentParams(), token, webhookId, changeSourceId);

    PagerDutyWebhook pagerDutyWebhook = (PagerDutyWebhook) hPersistence.createQuery(Webhook.class).get();
    assertThat(pagerDutyWebhook).isNotNull();
    assertThat(pagerDutyWebhook.getAccountId()).isEqualTo(context.getAccountId());
    assertThat(pagerDutyWebhook.getOrgIdentifier()).isEqualTo(context.getOrgIdentifier());
    assertThat(pagerDutyWebhook.getProjectIdentifier()).isEqualTo(context.getProjectIdentifier());
    assertThat(pagerDutyWebhook.getServiceIdentifier()).isEqualTo(context.getServiceIdentifier());
    assertThat(pagerDutyWebhook.getEnvIdentifier()).isEqualTo(context.getEnvIdentifier());
    assertThat(pagerDutyWebhook.getToken()).isEqualTo(token);
    assertThat(pagerDutyWebhook.getWebhookId()).isEqualTo(webhookId);
    assertThat(pagerDutyWebhook.getPagerdutyChangeSourceId()).isEqualTo(changeSourceId);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_deleteWebhook() {
    webhookService.createPagerdutyWebhook(context.getServiceEnvironmentParams(), token, webhookId, changeSourceId);

    PagerDutyWebhook pagerDutyWebhook = (PagerDutyWebhook) hPersistence.createQuery(Webhook.class).get();
    assertThat(pagerDutyWebhook).isNotNull();
    webhookService.deleteWebhook(pagerDutyWebhook);

    Webhook webhook = hPersistence.createQuery(Webhook.class).get();
    assertThat(webhook).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_handlePagerDutyEvent() {
    PagerDutyChangeSource changeSource = builderFactory.getPagerDutyChangeSourceBuilder().build();
    changeSource.setIdentifier(changeSourceId);
    hPersistence.save(changeSource);

    webhookService.createPagerdutyWebhook(context.getServiceEnvironmentParams(), token, webhookId, changeSourceId);
    PagerDutyWebhook pagerDutyWebhook = (PagerDutyWebhook) hPersistence.createQuery(Webhook.class).get();
    assertThat(pagerDutyWebhook).isNotNull();

    PagerDutyIncidentDTO incidentDTO = PagerDutyIncidentDTO.builder()
                                           .id(randomAlphabetic(20))
                                           .self(randomAlphabetic(20))
                                           .title(randomAlphabetic(20))
                                           .build();
    webhookService.handlePagerDutyWebhook(token, incidentDTO);

    Activity activity = hPersistence.createQuery(Activity.class).get();
    assertThat(activity).isNotNull();
    assertThat(activity.getType()).isEqualByComparingTo(ActivityType.PAGER_DUTY);
    assertThat(activity.getActivityName()).isEqualTo(incidentDTO.getTitle());
    assertThat(((PagerDutyActivity) activity).getPagerDutyUrl()).isEqualTo(incidentDTO.getSelf());
    assertThat(((PagerDutyActivity) activity).getEventId()).isEqualTo(incidentDTO.getId());
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void test_handlePagerDutyEvent_duplicateEvents() {
    PagerDutyChangeSource changeSource = builderFactory.getPagerDutyChangeSourceBuilder().build();
    changeSource.setIdentifier(changeSourceId);
    hPersistence.save(changeSource);

    webhookService.createPagerdutyWebhook(context.getServiceEnvironmentParams(), token, webhookId, changeSourceId);
    PagerDutyWebhook pagerDutyWebhook = (PagerDutyWebhook) hPersistence.createQuery(Webhook.class).get();
    assertThat(pagerDutyWebhook).isNotNull();

    PagerDutyIncidentDTO incidentDTO = PagerDutyIncidentDTO.builder()
                                           .id(randomAlphabetic(20))
                                           .self(randomAlphabetic(20))
                                           .title(randomAlphabetic(20))
                                           .build();
    webhookService.handlePagerDutyWebhook(token, incidentDTO);

    List<Activity> activity = hPersistence.createQuery(Activity.class).asList();
    assertThat(activity.size()).isEqualTo(1);
    webhookService.handlePagerDutyWebhook(token, incidentDTO);
    activity = hPersistence.createQuery(Activity.class).asList();
    assertThat(activity.size()).isEqualTo(1);
  }
}
