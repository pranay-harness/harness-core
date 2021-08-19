package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.rule.OwnerRule.MOUNIK;

import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.CgEventConfig;
import io.harness.beans.CgEventRule;
import io.harness.beans.WebHookEventConfig;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import java.util.LinkedList;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@Slf4j
@OwnedBy(CDC)
public class EventConfigServiceTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  public static final String GLOBAL_APP_ID = "__GLOBAL_APP_ID__";
  public static final String GLOBAL_ACCOUNT_ID = "__GLOBAL_ACCOUNT_ID__";

  @Mock private HPersistence hPersistence;
  @InjectMocks private EventConfigServiceImpl eventConfigService;

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void createEventsConfigAllValidations() {
    // Null Config
    CgEventConfig eventConfig = new CgEventConfig();
    eventConfig.setName("config1");
    Assertions
        .assertThatThrownBy(() -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, eventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Event config requires rule to be specified");
    // No Config
    CgEventRule eventRule = new CgEventRule();
    eventRule.setType(CgEventRule.CgRuleType.ALL);
    CgEventConfig cgEventConfig = CgEventConfig.builder()
                                      .name("config2")
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .appId(GLOBAL_APP_ID)
                                      .enabled(true)
                                      .rule(eventRule)
                                      .build();
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Http details for configuration is required!");
    // No URL
    WebHookEventConfig config = new WebHookEventConfig();
    cgEventConfig.setConfig(config);
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("URL Required: URL field is blank!");
    cgEventConfig.getConfig().setUrl("url1");
    // RuleType ALL and successful create
    eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig);
    // Rule Type PIPELINE but no Rule
    cgEventConfig.getRule().setType(CgEventRule.CgRuleType.PIPELINE);
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("For Event rule type Pipeline rule need be declared");
    // Rule Type PIPELINE, all pipelines, all events
    CgEventRule.PipelineRule pipelineRule = new CgEventRule.PipelineRule();
    pipelineRule.setAllPipelines(true);
    pipelineRule.setAllEvents(true);
    cgEventConfig.getRule().setPipelineRule(pipelineRule);
    eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig);
    // All events false but empty events list
    pipelineRule.setAllEvents(false);
    pipelineRule.setEvents(new LinkedList<>());
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("For Event rule type Pipeline choose all events or specify at least one event");
    // All pipelines false but empty pipelineIds list
    pipelineRule.setAllPipelines(false);
    pipelineRule.setAllEvents(true);
    pipelineRule.setPipelineIds(new LinkedList<>());
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("For Event rule type Pipeline choose all pipelines or specify at least one pipeline");
  }

  @Test
  @Owner(developers = MOUNIK)
  @Category(UnitTests.class)
  public void updateEventsConfigAllValidations() {
    CgEventRule eventRule = new CgEventRule();
    eventRule.setType(CgEventRule.CgRuleType.ALL);
    CgEventConfig cgEventConfig = CgEventConfig.builder()
                                      .name("config")
                                      .accountId(GLOBAL_ACCOUNT_ID)
                                      .appId(GLOBAL_APP_ID)
                                      .enabled(true)
                                      .rule(eventRule)
                                      .build();
    WebHookEventConfig config = new WebHookEventConfig();
    config.setUrl("url1");
    cgEventConfig.setConfig(config);
    cgEventConfig.setUuid("uuid1");
    eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig);
    cgEventConfig.setUuid("uuid2");
    cgEventConfig.setName("config2");
    eventConfigService.createEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig);
    cgEventConfig.setUuid("uuid3");
    when(eventConfigService.getEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, "uuid3")).thenReturn(cgEventConfig);
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.updateEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Failed to update event config: No such event config exists");
    CgEventConfig cgEventConfig2 = CgEventConfig.builder()
                                       .name("config")
                                       .accountId(GLOBAL_ACCOUNT_ID)
                                       .appId(GLOBAL_APP_ID)
                                       .enabled(true)
                                       .rule(eventRule)
                                       .build();
    cgEventConfig2.setConfig(config);
    cgEventConfig2.setUuid("uuid2");
    Assertions
        .assertThatThrownBy(
            () -> eventConfigService.updateEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig2))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Duplicate Name " + cgEventConfig2.getName());
    cgEventConfig2.setName("config3");
    eventConfigService.updateEventsConfig(GLOBAL_ACCOUNT_ID, GLOBAL_APP_ID, cgEventConfig2);
  }
}
