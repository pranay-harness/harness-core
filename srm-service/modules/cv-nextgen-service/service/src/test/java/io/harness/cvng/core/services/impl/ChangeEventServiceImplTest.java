/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ABHIJITH;
import static io.harness.rule.OwnerRule.ARPITJ;
import static io.harness.rule.OwnerRule.KAMAL;
import static io.harness.rule.OwnerRule.VARSHA_LALWANI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.offset;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.beans.change.DeepLink;
import io.harness.cvng.beans.change.InternalChangeEvent;
import io.harness.cvng.beans.change.InternalChangeEventMetaData;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.ChangeTimeline;
import io.harness.cvng.core.beans.change.ChangeTimeline.TimeRangeDetail;
import io.harness.cvng.core.beans.monitoredService.DurationDTO;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.core.services.impl.ChangeEventServiceImpl.TimelineObject;
import io.harness.cvng.utils.ScopedInformation;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mongodb.morphia.query.Query;

public class ChangeEventServiceImplTest extends CvNextGenTestBase {
  @Inject MonitoredServiceService monitoredServiceService;
  @Inject ChangeEventServiceImpl changeEventService;
  @Inject ChangeSourceService changeSourceService;
  @Inject HPersistence hPersistence;

  BuilderFactory builderFactory;

  List<String> changeSourceIdentifiers = Arrays.asList("changeSourceID");

  @Before
  public void before() {
    builderFactory = BuilderFactory.getDefault();
    monitoredServiceService.createDefault(builderFactory.getProjectParams(),
        builderFactory.getContext().getServiceIdentifier(), builderFactory.getContext().getEnvIdentifier());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_insert() {
    changeSourceService.create(builderFactory.getContext().getMonitoredServiceParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.harnessCDChangeEventDTOBuilder().build();

    changeEventService.register(changeEventDTO);

    Activity activityFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(activityFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testRegister_insertInternalChangeEvent() {
    ChangeEventDTO changeEventDTO = builderFactory.getInternalChangeEventDTO_FFBuilder().build();

    changeEventService.register(changeEventDTO);

    Activity activityFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(activityFromDb).isNotNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testRegister_insertWithNoMonitoredService() {
    changeSourceService.create(builderFactory.getContext().getMonitoredServiceParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO =
        builderFactory.harnessCDChangeEventDTOBuilder().monitoredServiceIdentifier(null).build();

    boolean saved = changeEventService.register(changeEventDTO);
    assertThat(saved).isTrue();
    Activity activityFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(activityFromDb).isNotNull();
    assertThat(activityFromDb.getMonitoredServiceIdentifier())
        .isEqualTo(builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier());
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testRegister_insertWithNoMonitoredServiceInternalChangeEvent() {
    ChangeEventDTO changeEventDTO =
        builderFactory.getInternalChangeEventDTO_FFBuilder().monitoredServiceIdentifier(null).build();

    boolean saved = changeEventService.register(changeEventDTO);
    assertThat(saved).isTrue();
    Activity activityFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(activityFromDb).isNotNull();
    assertThat(activityFromDb.getMonitoredServiceIdentifier())
        .isEqualTo(builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier());
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_update() {
    changeSourceService.create(builderFactory.getContext().getMonitoredServiceParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));

    ChangeEventDTO changeEventDTO = builderFactory.harnessCDChangeEventDTOBuilder().build();
    changeEventService.register(changeEventDTO);
    Long eventTime = 123L;
    ChangeEventDTO changeEventDTO2 = builderFactory.harnessCDChangeEventDTOBuilder().eventTime(eventTime).build();
    changeEventService.register(changeEventDTO2);

    Assertions.assertThat(hPersistence.createQuery(Activity.class).count()).isEqualTo(1);
    Activity changeEventFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(changeEventFromDb.getEventTime().toEpochMilli()).isEqualTo(eventTime);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testRegister_multipleInternalChangeEvent() {
    ChangeEventDTO changeEventDTO = builderFactory.getInternalChangeEventDTO_FFBuilder().eventTime(100L).build();
    changeEventService.register(changeEventDTO);
    Long eventTime = 123L;
    ChangeEventDTO changeEventDTO2 = builderFactory.getInternalChangeEventDTO_FFBuilder().eventTime(eventTime).build();
    changeEventService.register(changeEventDTO2);

    Assertions.assertThat(hPersistence.createQuery(Activity.class).count()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testRegister_updateInternalChangeEvent() {
    ChangeEventDTO changeEventDTO = builderFactory.getInternalChangeEventDTO_FFBuilder().build();
    changeEventService.register(changeEventDTO);
    ChangeEventDTO changeEventDTO2 =
        builderFactory.getInternalChangeEventDTO_FFBuilder()
            .metadata(
                InternalChangeEventMetaData.builder()
                    .activityType(ActivityType.FEATURE_FLAG)
                    .updatedBy("user2")
                    .eventStartTime(1000l)
                    .internalChangeEvent(
                        InternalChangeEvent.builder()
                            .changeEventDetailsLink(DeepLink.builder()
                                                        .action(DeepLink.Action.FETCH_DIFF_DATA)
                                                        .url("changeEventDetails")
                                                        .build())
                            .internalLinkToEntity(
                                DeepLink.builder().action(DeepLink.Action.REDIRECT_URL).url("internalUrl").build())
                            .eventDescriptions(Arrays.asList("eventDesc1", "eventDesc2"))
                            .build())
                    .build())
            .build();
    changeEventService.register(changeEventDTO2);

    Assertions.assertThat(hPersistence.createQuery(Activity.class).count()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_noChangeSource() {
    changeSourceService.create(builderFactory.getContext().getMonitoredServiceParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.harnessCDChangeEventDTOBuilder().build();

    changeEventService.register(changeEventDTO);

    Activity changeEventFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(changeEventFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetPaginated() {
    Activity harnessCDActivity_1 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build();
    Activity harnessCDActivity_2 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(200)).build();
    Activity harnessCDActivity_3 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build();
    hPersistence.save(Arrays.asList(harnessCDActivity_1, harnessCDActivity_2, harnessCDActivity_3));
    PageResponse<ChangeEventDTO> firstPage = changeEventService.getChangeEvents(
        builderFactory.getContext().getProjectParams(), null, null, null, null, null, Instant.ofEpochSecond(100),
        Instant.ofEpochSecond(400), PageRequest.builder().pageIndex(0).pageSize(2).build());
    PageResponse<ChangeEventDTO> secondPage = changeEventService.getChangeEvents(
        builderFactory.getContext().getProjectParams(), null, null, null, null, null, Instant.ofEpochSecond(100),
        Instant.ofEpochSecond(400), PageRequest.builder().pageIndex(1).pageSize(2).build());

    assertThat(firstPage.getPageIndex()).isEqualTo(0);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getTotalItems()).isEqualTo(3);
    assertThat(firstPage.getTotalPages()).isEqualTo(2);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getContent().size()).isEqualTo(2);
    assertThat(firstPage.getContent().get(0).getEventTime()).isEqualTo(300000);
    assertThat(firstPage.getContent().get(1).getEventTime()).isEqualTo(200000);
    assertThat(secondPage.getContent().get(0).getEventTime()).isEqualTo(100000);
    assertThat(secondPage.getPageIndex()).isEqualTo(1);
    assertThat(secondPage.getPageItemCount()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetPaginated_withServiceFiltering() {
    Activity harnessCDActivity_1 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build();
    Activity harnessCDActivity_2 = builderFactory.getDeploymentActivityBuilder()
                                       .monitoredServiceIdentifier("service2_env2")
                                       .eventTime(Instant.ofEpochSecond(200))
                                       .build();
    Activity harnessCDActivity_3 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build();
    hPersistence.save(Arrays.asList(harnessCDActivity_1, harnessCDActivity_2, harnessCDActivity_3));
    PageResponse<ChangeEventDTO> firstPage = changeEventService.getChangeEvents(
        builderFactory.getContext().getProjectParams(),
        Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null, null, null, null,
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(400), PageRequest.builder().pageIndex(0).pageSize(2).build());

    assertThat(firstPage.getPageIndex()).isEqualTo(0);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getTotalItems()).isEqualTo(2);
    assertThat(firstPage.getTotalPages()).isEqualTo(1);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getContent().size()).isEqualTo(2);
    assertThat(firstPage.getContent().get(0).getEventTime()).isEqualTo(300000);
    assertThat(firstPage.getContent().get(1).getEventTime()).isEqualTo(100000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetPaginated_withTypeFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getPagerDutyActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    PageResponse<ChangeEventDTO> firstPage =
        changeEventService.getChangeEvents(builderFactory.getContext().getProjectParams(),
            Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null, null,
            Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
            Arrays.asList(ChangeSourceType.HARNESS_CD, ChangeSourceType.KUBERNETES), Instant.ofEpochSecond(100),
            Instant.ofEpochSecond(400), PageRequest.builder().pageIndex(0).pageSize(2).build());

    assertThat(firstPage.getPageIndex()).isEqualTo(0);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getTotalItems()).isEqualTo(2);
    assertThat(firstPage.getTotalPages()).isEqualTo(1);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getContent().size()).isEqualTo(2);
    assertThat(firstPage.getContent().get(0).getEventTime()).isEqualTo(300000);
    assertThat(firstPage.getContent().get(1).getEventTime()).isEqualTo(100000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testCreateTextSearchQuery() {
    // testing query as our test MongoServer doesn't support text search:
    // https://github.com/bwaldvogel/mongo-java-server
    Query<Activity> activityQuery = changeEventService.createTextSearchQuery(Instant.ofEpochSecond(100),
        Instant.ofEpochSecond(400), "searchText", Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
        Arrays.asList(ChangeSourceType.HARNESS_CD, ChangeSourceType.KUBERNETES));

    assertThat(activityQuery.toString())
        .isEqualTo(
            "{ query: {\"$and\": [{\"$text\": {\"$search\": \"searchText\"}}, {\"eventTime\": {\"$lt\": {\"$date\": 400000}}}, {\"eventTime\": {\"$gte\": {\"$date\": 100000}}}, {\"type\": {\"$in\": [\"DEPLOYMENT\"]}}]}  }");

    activityQuery = changeEventService.createTextSearchQuery(
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(400), "searchText", null, null);
    assertThat(activityQuery.toString())
        .isEqualTo(
            "{ query: {\"$and\": [{\"$text\": {\"$search\": \"searchText\"}}, {\"eventTime\": {\"$lt\": {\"$date\": 400000}}}, {\"eventTime\": {\"$gte\": {\"$date\": 100000}}}, {\"type\": {\"$in\": [\"DEPLOYMENT\", \"PAGER_DUTY\", \"KUBERNETES\", \"HARNESS_CD_CURRENT_GEN\", \"FEATURE_FLAG\"]}}]}  }");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary() {
    hPersistence.save(Arrays.asList(
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(200)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(400)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(), (List<String>) null, null,
            null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(3);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getPercentageChange())
        .isCloseTo(200.0, offset(0.1));
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getPercentageChange())
        .isCloseTo(100.0, offset(0.1));
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getPercentageChange())
        .isCloseTo(0.0, offset(0.1));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCount()).isEqualTo(2);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getPercentageChange())
        .isCloseTo(100.0, offset(0.1));

    assertThat(changeSummaryDTO.getTotal().getCount()).isEqualTo(6);
    assertThat(changeSummaryDTO.getTotal().getCountInPrecedingWindow()).isEqualTo(3);
    assertThat(changeSummaryDTO.getTotal().getPercentageChange()).isCloseTo(100.0, offset(0.1));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(50))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(250)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(250))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(),
            Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null, null, null,
            Instant.ofEpochSecond(100), Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(2);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getPercentageChange())
        .isCloseTo(100.0, offset(0.1));
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getPercentageChange())
        .isCloseTo(100.0, offset(0.1));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getPercentageChange())
        .isCloseTo(0.0, offset(0.1));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getPercentageChange())
        .isCloseTo(0.0, offset(0.1));

    assertThat(changeSummaryDTO.getTotal().getCount()).isEqualTo(4);
    assertThat(changeSummaryDTO.getTotal().getCountInPrecedingWindow()).isEqualTo(3);
    assertThat(changeSummaryDTO.getTotal().getPercentageChange()).isCloseTo(33.33, offset(0.1));
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withTypeFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service2_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getPagerDutyActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(),
            Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null,
            Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
            Arrays.asList(ChangeSourceType.HARNESS_CD, ChangeSourceType.KUBERNETES), Instant.ofEpochSecond(100),
            Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(0);
  }

  @Test
  @Owner(developers = ARPITJ)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withTypeFilteringInternalChangeSource() {
    hPersistence.save(Arrays.asList(
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(50))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service2_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(250)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(250))
            .build(),
        builderFactory.getPagerDutyActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(),
            Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null,
            Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.FEATURE_FLAG),
            Arrays.asList(ChangeSourceType.HARNESS_FF, ChangeSourceType.KUBERNETES), Instant.ofEpochSecond(100),
            Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCountInPrecedingWindow())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withEnvironmentFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(50))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(250)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(250))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(), (List<String>) null,
            Arrays.asList(builderFactory.getContext().getEnvIdentifier()), null, null, Instant.ofEpochSecond(100),
            Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(2);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(1);

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.FEATURE_FLAG).getPercentageChange())
        .isEqualTo(0);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(200)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(250)).build(),
        builderFactory.getInternalChangeActivity_FFBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        null, null, false, null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(300000);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> featureFlagChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.FEATURE_FLAG);
    assertThat(featureFlagChanges.size()).isEqualTo(2);
    assertThat(featureFlagChanges.get(0).getCount()).isEqualTo(1);
    assertThat(featureFlagChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(featureFlagChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(featureFlagChanges.get(1).getCount()).isEqualTo(1);
    assertThat(featureFlagChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(featureFlagChanges.get(1).getEndTime()).isEqualTo(500000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetMonitoredServiceChangeTimeline() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service1_env1")
            .eventTime(Instant.ofEpochSecond(500))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(14398)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(14399500))
            .build()));
    ChangeTimeline changeTimeline =
        changeEventService.getMonitoredServiceChangeTimeline(builderFactory.getContext().getMonitoredServiceParams(),
            null, null, DurationDTO.FOUR_HOURS, Instant.ofEpochSecond(14398));

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(0);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(14100000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(14400000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(0);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(300000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline_withTypeFilters() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(200)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getPagerDutyActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        null, null, false, null, Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
        Arrays.asList(ChangeSourceType.HARNESS_CD, ChangeSourceType.KUBERNETES), Instant.ofEpochSecond(100),
        Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges).isEmpty();
    List<TimeRangeDetail> alertChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.ALERTS);
    assertThat(alertChanges).isEmpty();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimelineObject_forAggregationValidation() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(200)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(600))
            .build()));

    Iterator<TimelineObject> changeTimelineObject =
        changeEventService.getTimelineObject(builderFactory.getContext().getProjectParams(), null, null, null, null,
            null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2, false);
    List<TimelineObject> timelineObjectList = new ArrayList<>();
    changeTimelineObject.forEachRemaining(timelineObject -> timelineObjectList.add(timelineObject));

    assertThat(timelineObjectList.size()).isEqualTo(3);
    assertThat(timelineObjectList.stream()
                   .filter(timelineObject
                       -> timelineObject.id.index.equals(0) && timelineObject.id.type.equals(ActivityType.DEPLOYMENT))
                   .findAny()
                   .get()
                   .count)
        .isEqualTo(2);
    assertThat(timelineObjectList.stream()
                   .filter(timelineObject
                       -> timelineObject.id.index.equals(1) && timelineObject.id.type.equals(ActivityType.DEPLOYMENT))
                   .findAny()
                   .get()
                   .count)
        .isEqualTo(1);
    assertThat(timelineObjectList.stream()
                   .filter(timelineObject
                       -> timelineObject.id.index.equals(1) && timelineObject.id.type.equals(ActivityType.KUBERNETES))
                   .findAny()
                   .get()
                   .count)
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline_withServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("monitoredservice2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(),
        Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null, null, false, null, null, null,
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(300000);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(500000);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTimeline_withMonitoredServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("monitoredservice2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        null, Arrays.asList(builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier()),
        false, null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(300000);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(500000);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetTimeline_withScopedMonitoredServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("monitoredservice2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline =
        changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null, null,
            Arrays.asList(ScopedInformation.getScopedInformation(builderFactory.getContext().getAccountId(),
                builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
                builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier())),
            true, null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);
    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(300000);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(500000);
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testGetTimeline_withMonitoredServiceAndServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("monitoredservice2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    assertThatThrownBy(
        ()
            -> changeEventService.getTimeline(builderFactory.getContext().getProjectParams(),
                Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null,
                Arrays.asList(builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier()),
                false, null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2))
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("serviceIdentifier, envIdentifier filter can not be used with monitoredServiceIdentifier filter");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline_withEnvironmentFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service2_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        Arrays.asList(builderFactory.getContext().getEnvIdentifier()), null, false, null, null, null,
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

    List<TimeRangeDetail> deploymentChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.DEPLOYMENT);
    assertThat(deploymentChanges.size()).isEqualTo(2);
    assertThat(deploymentChanges.get(0).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(0).getStartTime()).isEqualTo(100000);
    assertThat(deploymentChanges.get(0).getEndTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getCount()).isEqualTo(1);
    assertThat(deploymentChanges.get(1).getStartTime()).isEqualTo(300000);
    assertThat(deploymentChanges.get(1).getEndTime()).isEqualTo(500000);

    List<TimeRangeDetail> infrastructureChanges =
        changeTimeline.getCategoryTimeline().get(ChangeCategory.INFRASTRUCTURE);
    assertThat(infrastructureChanges.size()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getCount()).isEqualTo(1);
    assertThat(infrastructureChanges.get(0).getStartTime()).isEqualTo(300000);
    assertThat(infrastructureChanges.get(0).getEndTime()).isEqualTo(500000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetPaginated_withEnvironmentFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .monitoredServiceIdentifier("service_env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    PageResponse<ChangeEventDTO> firstPage =
        changeEventService.getChangeEvents(builderFactory.getContext().getProjectParams(), null,
            Arrays.asList(builderFactory.getContext().getEnvIdentifier()), null, null, null, Instant.ofEpochSecond(100),
            Instant.ofEpochSecond(400), PageRequest.builder().pageIndex(0).pageSize(2).build());

    assertThat(firstPage.getPageIndex()).isEqualTo(0);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getTotalItems()).isEqualTo(2);
    assertThat(firstPage.getTotalPages()).isEqualTo(1);
    assertThat(firstPage.getPageItemCount()).isEqualTo(2);
    assertThat(firstPage.getContent().size()).isEqualTo(2);
    assertThat(firstPage.getContent().get(0).getEventTime()).isEqualTo(300000);
    assertThat(firstPage.getContent().get(1).getEventTime()).isEqualTo(100000);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_WithServiceParams() {
    hPersistence.save(builderFactory.getDeploymentActivityBuilder().build());
    hPersistence.save(builderFactory.getDeploymentActivityBuilder()
                          .eventTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(15)))
                          .build());
    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getMonitoredServiceParams(),
            changeSourceIdentifiers, builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
            builderFactory.getClock().instant().plus(Duration.ofMinutes(10)));
    Assertions.assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount())
        .isEqualTo(1);
    Assertions
        .assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetChangeSummary_WithMonitoredService() {
    hPersistence.save(builderFactory.getDeploymentActivityBuilder().build());
    hPersistence.save(builderFactory.getDeploymentActivityBuilder()
                          .eventTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(15)))
                          .build());
    ChangeSummaryDTO changeSummaryDTO = changeEventService.getChangeSummary(builderFactory.getProjectParams(), null,
        Collections.singletonList(
            builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier()),
        false, null, null, builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
        builderFactory.getClock().instant().plus(Duration.ofMinutes(10)));
    Assertions.assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount())
        .isEqualTo(1);
    Assertions
        .assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = VARSHA_LALWANI)
  @Category(UnitTests.class)
  public void testGetChangeSummary_WithScopedMonitoredService() {
    hPersistence.save(builderFactory.getDeploymentActivityBuilder().build());
    hPersistence.save(builderFactory.getDeploymentActivityBuilder()
                          .eventTime(builderFactory.getClock().instant().minus(Duration.ofMinutes(15)))
                          .build());
    ChangeSummaryDTO changeSummaryDTO = changeEventService.getChangeSummary(builderFactory.getProjectParams(), null,
        Collections.singletonList(ScopedInformation.getScopedInformation(builderFactory.getContext().getAccountId(),
            builderFactory.getContext().getOrgIdentifier(), builderFactory.getContext().getProjectIdentifier(),
            builderFactory.getContext().getMonitoredServiceParams().getMonitoredServiceIdentifier())),
        true, null, null, builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
        builderFactory.getClock().instant().plus(Duration.ofMinutes(10)));
    Assertions.assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount())
        .isEqualTo(1);
    Assertions
        .assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
  }
}
