/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.cvng.core.services.impl;

import static io.harness.rule.OwnerRule.ABHIJITH;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;

import io.harness.CvNextGenTestBase;
import io.harness.category.element.UnitTests;
import io.harness.cvng.BuilderFactory;
import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.DeploymentActivity;
import io.harness.cvng.beans.activity.ActivityType;
import io.harness.cvng.beans.change.ChangeCategory;
import io.harness.cvng.beans.change.ChangeEventDTO;
import io.harness.cvng.beans.change.ChangeSourceType;
import io.harness.cvng.client.NextGenService;
import io.harness.cvng.core.beans.change.ChangeSummaryDTO;
import io.harness.cvng.core.beans.change.ChangeTimeline;
import io.harness.cvng.core.beans.change.ChangeTimeline.TimeRangeDetail;
import io.harness.cvng.core.beans.params.ProjectParams;
import io.harness.cvng.core.services.api.monitoredService.ChangeSourceService;
import io.harness.cvng.core.services.impl.ChangeEventServiceImpl.TimelineObject;
import io.harness.cvng.core.transformer.changeEvent.ChangeEventMetaDataTransformer;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.ng.core.environment.dto.EnvironmentResponseDTO;
import io.harness.ng.core.service.dto.ServiceResponseDTO;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;

import com.google.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mockito;
import org.mongodb.morphia.query.Query;

public class ChangeEventServiceImplTest extends CvNextGenTestBase {
  @Inject ChangeEventServiceImpl changeEventService;
  @Inject ChangeSourceService changeSourceService;
  @Inject private Map<ChangeSourceType, ChangeEventMetaDataTransformer> changeTypeMetaDataTransformerMap;
  @Inject HPersistence hPersistence;

  BuilderFactory builderFactory;

  List<String> changeSourceIdentifiers = Arrays.asList("changeSourceID");

  @Before
  public void before() throws IllegalAccessException {
    NextGenService nextGenService = Mockito.mock(NextGenService.class);
    for (ChangeEventMetaDataTransformer transformer : changeTypeMetaDataTransformerMap.values()) {
      FieldUtils.writeField(transformer, "nextGenService", nextGenService, true);
    }
    Mockito.when(nextGenService.getService(any(), any(), any(), any()))
        .thenReturn(ServiceResponseDTO.builder().name("serviceName").build());
    Mockito.when(nextGenService.getEnvironment(any(), any(), any(), any()))
        .thenReturn(EnvironmentResponseDTO.builder().name("environmentName").build());
    builderFactory = BuilderFactory.getDefault();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_insert() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();

    changeEventService.register(changeEventDTO);

    Activity activityFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(activityFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_update() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));

    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();
    changeEventService.register(changeEventDTO);
    Long eventTime = 123L;
    ChangeEventDTO changeEventDTO2 = builderFactory.getHarnessCDChangeEventDTOBuilder().eventTime(eventTime).build();
    changeEventService.register(changeEventDTO2);

    Assertions.assertThat(hPersistence.createQuery(Activity.class).count()).isEqualTo(1);
    Activity changeEventFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(changeEventFromDb.getEventTime().toEpochMilli()).isEqualTo(eventTime);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testRegister_noChangeSource() {
    changeSourceService.create(builderFactory.getContext().getServiceEnvironmentParams(),
        new HashSet<>(Arrays.asList(builderFactory.getHarnessCDChangeSourceDTOBuilder().build())));
    ChangeEventDTO changeEventDTO = builderFactory.getHarnessCDChangeEventDTOBuilder().build();

    changeEventService.register(changeEventDTO);

    Activity changeEventFromDb = hPersistence.createQuery(Activity.class).get();
    Assertions.assertThat(changeEventFromDb).isNotNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGet_withoutCategory() {
    DeploymentActivity harnessCDActivity = builderFactory.getDeploymentActivityBuilder().build();
    hPersistence.save(harnessCDActivity);

    List<ChangeEventDTO> changeEventDTOS =
        changeEventService.get(builderFactory.getContext().getServiceEnvironmentParams(), changeSourceIdentifiers,
            builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
            builderFactory.getClock().instant().plus(Duration.ofMinutes(10)), null);
    Assertions.assertThat(changeEventDTOS.size()).isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetPaginated() {
    Activity harnessCDActivity_1 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build();
    Activity harnessCDActivity_2 = builderFactory.getDeploymentActivityBuilder()
                                       .serviceIdentifier("service2")
                                       .environmentIdentifier("env2")
                                       .eventTime(Instant.ofEpochSecond(200))
                                       .build();
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
                                       .serviceIdentifier("service2")
                                       .environmentIdentifier("env2")
                                       .eventTime(Instant.ofEpochSecond(200))
                                       .build();
    Activity harnessCDActivity_3 =
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build();
    hPersistence.save(Arrays.asList(harnessCDActivity_1, harnessCDActivity_2, harnessCDActivity_3));
    PageResponse<ChangeEventDTO> firstPage = changeEventService.getChangeEvents(
        builderFactory.getContext().getProjectParams(), Arrays.asList(harnessCDActivity_1.getServiceIdentifier()), null,
        null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(400),
        PageRequest.builder().pageIndex(0).pageSize(2).build());

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
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
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
    // testing query as our test MongoServer doesnt support text search: https://github.com/bwaldvogel/mongo-java-server
    Query<Activity> activityQuery = changeEventService.createTextSearchQuery(ProjectParams.builder()
                                                                                 .accountIdentifier("accountId")
                                                                                 .orgIdentifier("orgIdentifier")
                                                                                 .projectIdentifier("projectIdentifier")
                                                                                 .build(),
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(400), Arrays.asList("service"), Arrays.asList("env"),
        "searchText", Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
        Arrays.asList(ChangeSourceType.HARNESS_CD, ChangeSourceType.KUBERNETES));

    assertThat(activityQuery.toString())
        .isEqualTo(
            "{ query: {\"$and\": [{\"accountId\": \"accountId\"}, {\"orgIdentifier\": \"orgIdentifier\"}, {\"projectIdentifier\": \"projectIdentifier\"}, {\"$text\": {\"$search\": \"searchText\"}}, {\"eventTime\": {\"$lt\": {\"$date\": 400000}}}, {\"eventTime\": {\"$gte\": {\"$date\": 100000}}}, {\"type\": {\"$in\": [\"DEPLOYMENT\"]}}, {\"$or\": [{\"serviceIdentifier\": {\"$in\": [\"service\"]}}, {\"relatedAppServices.serviceIdentifier\": {\"$in\": [\"service\"]}}]}, {\"$or\": [{\"environmentIdentifier\": {\"$in\": [\"env\"]}}, {\"relatedAppServices.environmentIdentifier\": {\"$in\": [\"env\"]}}]}]}  }");

    activityQuery = changeEventService.createTextSearchQuery(ProjectParams.builder()
                                                                 .accountIdentifier("accountId")
                                                                 .orgIdentifier("orgIdentifier")
                                                                 .projectIdentifier("projectIdentifier")
                                                                 .build(),
        Instant.ofEpochSecond(100), Instant.ofEpochSecond(400), null, null, "searchText", null, null);
    assertThat(activityQuery.toString())
        .isEqualTo(
            "{ query: {\"$and\": [{\"accountId\": \"accountId\"}, {\"orgIdentifier\": \"orgIdentifier\"}, {\"projectIdentifier\": \"projectIdentifier\"}, {\"$text\": {\"$search\": \"searchText\"}}, {\"eventTime\": {\"$lt\": {\"$date\": 400000}}}, {\"eventTime\": {\"$gte\": {\"$date\": 100000}}}, {\"type\": {\"$in\": [\"DEPLOYMENT\", \"PAGER_DUTY\", \"KUBERNETES\", \"HARNESS_CD_CURRENT_GEN\"]}}]}  }");
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(), null, null, null, null,
            Instant.ofEpochSecond(100), Instant.ofEpochSecond(500));

    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount()).isEqualTo(3);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(1);
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withServiceFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
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
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCount()).isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.ALERTS).getCountInPrecedingWindow())
        .isEqualTo(0);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCount()).isEqualTo(1);
    assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.INFRASTRUCTURE).getCountInPrecedingWindow())
        .isEqualTo(1);
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
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
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
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetChangeSummary_withEnvironmentFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));

    ChangeSummaryDTO changeSummaryDTO =
        changeEventService.getChangeSummary(builderFactory.getContext().getProjectParams(), null,
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
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        null, null, null, null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);

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
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimeline_withTypeFilters() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getPagerDutyActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        null, null, Arrays.asList(ChangeCategory.DEPLOYMENT, ChangeCategory.ALERTS),
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
    assertThat(infrastructureChanges).isNull();
    List<TimeRangeDetail> alertChanges = changeTimeline.getCategoryTimeline().get(ChangeCategory.ALERTS);
    assertThat(alertChanges).isNull();
  }

  @Test
  @Owner(developers = ABHIJITH)
  @Category(UnitTests.class)
  public void testGetTimelineObject_forAggregationValidation() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(600))
            .build()));

    Iterator<TimelineObject> changeTimelineObject =
        changeEventService.getTimelineObject(builderFactory.getContext().getProjectParams(), null, null, null, null,
            null, Instant.ofEpochSecond(100), Instant.ofEpochSecond(500), 2);
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
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(),
        Arrays.asList(builderFactory.getContext().getServiceIdentifier()), null, null, null, null,
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
  public void testGetTimeline_withEnvironmentFiltering() {
    hPersistence.save(Arrays.asList(
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder().eventTime(Instant.ofEpochSecond(50)).build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(100)).build(),
        builderFactory.getDeploymentActivityBuilder()
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
            .eventTime(Instant.ofEpochSecond(200))
            .build(),
        builderFactory.getDeploymentActivityBuilder().eventTime(Instant.ofEpochSecond(300)).build(),
        builderFactory.getKubernetesClusterActivityForAppServiceBuilder()
            .eventTime(Instant.ofEpochSecond(300))
            .build()));
    ChangeTimeline changeTimeline = changeEventService.getTimeline(builderFactory.getContext().getProjectParams(), null,
        Arrays.asList(builderFactory.getContext().getEnvIdentifier()), null, null, null, Instant.ofEpochSecond(100),
        Instant.ofEpochSecond(500), 2);

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
            .serviceIdentifier("service2")
            .environmentIdentifier("env2")
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
  public void testGet_withCategory() {
    DeploymentActivity harnessCDActivity = builderFactory.getDeploymentActivityBuilder().build();
    hPersistence.save(harnessCDActivity);

    List<ChangeEventDTO> changeEventDTOS =
        changeEventService.get(builderFactory.getContext().getServiceEnvironmentParams(), changeSourceIdentifiers,
            builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
            builderFactory.getClock().instant().plus(Duration.ofMinutes(10)), Arrays.asList(ChangeCategory.DEPLOYMENT));
    Assertions.assertThat(changeEventDTOS.size()).isEqualTo(1);
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
        changeEventService.getChangeSummary(builderFactory.getContext().getServiceEnvironmentParams(),
            changeSourceIdentifiers, builderFactory.getClock().instant().minus(Duration.ofMinutes(10)),
            builderFactory.getClock().instant().plus(Duration.ofMinutes(10)));
    Assertions.assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCount())
        .isEqualTo(1);
    Assertions
        .assertThat(changeSummaryDTO.getCategoryCountMap().get(ChangeCategory.DEPLOYMENT).getCountInPrecedingWindow())
        .isEqualTo(1);
  }
}
