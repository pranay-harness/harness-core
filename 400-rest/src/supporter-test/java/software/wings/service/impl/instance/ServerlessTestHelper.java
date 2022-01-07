/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import software.wings.beans.EntityType;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats.AggregateInvocationCount;
import software.wings.dl.WingsPersistence;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.mongodb.morphia.query.UpdateOperations;

public class ServerlessTestHelper {
  public static final String ACCOUNTID = "accountid";

  @Data
  @Builder
  public static class Mocks {
    AggregationPipeline aggregationPipelineMock;
    Query queryMock;
    FieldEnd fieldEndMock;
    CriteriaContainer criteriaContainerMock;
    ServerlessInstance serverlessInstance;
    UpdateOperations updateOperationsMock;
    ServerlessInstanceStats serverlessInstanceStats;
  }

  public static Mocks setup_AggregationPipeline(WingsPersistence wingsPersistence) {
    AdvancedDatastore advancedDatastoreMock = mock(AdvancedDatastore.class);
    Query queryMock = mock(Query.class);
    final FieldEnd fieldEndMock = mock(FieldEnd.class);

    doReturn(advancedDatastoreMock).when(wingsPersistence).getDatastore(any(Class.class));
    doReturn(queryMock).when(wingsPersistence).createQuery(any(Class.class));
    doReturn(fieldEndMock).when(queryMock).field(anyString());
    final CriteriaContainer criteriaContainerMock = mock(CriteriaContainer.class);
    doReturn(criteriaContainerMock).when(queryMock).and(anyVararg());
    doReturn(criteriaContainerMock).when(queryMock).or(anyVararg());
    doReturn(fieldEndMock).when(queryMock).criteria(anyString());

    doReturn(queryMock).when(fieldEndMock).in(any(Iterable.class));
    doReturn(criteriaContainerMock).when(fieldEndMock).equal(any());
    doReturn(queryMock).when(fieldEndMock).lessThanOrEq(any());
    doReturn(criteriaContainerMock).when(fieldEndMock).greaterThanOrEq(any());
    doReturn(queryMock).when(fieldEndMock).lessThan(any());

    doReturn(queryMock).when(queryMock).project(anyString(), anyBoolean());
    doReturn(queryMock).when(queryMock).order(any(Sort.class));

    AggregationPipeline aggregationPipelineMOck = mock(AggregationPipeline.class);
    doReturn(aggregationPipelineMOck).when(advancedDatastoreMock).createAggregation(any(Class.class));

    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).match(any(Query.class));
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).group(anyListOf(Group.class), anyVararg());
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).group(anyString(), anyVararg());
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).skip(anyInt());
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).limit(anyInt());
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).sort(anyVararg());
    doReturn(aggregationPipelineMOck).when(aggregationPipelineMOck).project(anyVararg());

    return Mocks.builder()
        .aggregationPipelineMock(aggregationPipelineMOck)
        .criteriaContainerMock(criteriaContainerMock)
        .fieldEndMock(fieldEndMock)
        .queryMock(queryMock)
        .build();
  }

  public static ServerlessInstanceStats getServerlessInstanceStats() {
    return new ServerlessInstanceStats(Instant.now(), ACCOUNTID, getAggregateInvocationCounts());
  }
  public static List<AggregateInvocationCount> getAggregateInvocationCounts() {
    final AggregateInvocationCount invocationCount1 = AggregateInvocationCount.builder()
                                                          .entityType(EntityType.APPLICATION)
                                                          .name("agcount")
                                                          .id("agc1")
                                                          .invocationCount(10)
                                                          .invocationCountKey(InvocationCountKey.LAST_30_DAYS)
                                                          .build();
    final AggregateInvocationCount invocationCount2 = AggregateInvocationCount.builder()
                                                          .entityType(EntityType.SERVICE)
                                                          .name("agcount2")
                                                          .id("agc2")
                                                          .invocationCount(20)
                                                          .invocationCountKey(InvocationCountKey.LAST_30_DAYS)
                                                          .build();

    final AggregateInvocationCount invocationCount3 = AggregateInvocationCount.builder()
                                                          .entityType(EntityType.APPLICATION)
                                                          .name("agcount3")
                                                          .id("agc3")
                                                          .invocationCount(20)
                                                          .invocationCountKey(InvocationCountKey.SINCE_LAST_DEPLOYED)
                                                          .build();
    final AggregateInvocationCount invocationCount4 = AggregateInvocationCount.builder()
                                                          .entityType(EntityType.APPLICATION)
                                                          .name("agcount4")
                                                          .id("appid1")
                                                          .invocationCount(100)
                                                          .invocationCountKey(InvocationCountKey.LAST_30_DAYS)
                                                          .build();

    return Arrays.asList(invocationCount1, invocationCount2, invocationCount3, invocationCount4);
  }
}
