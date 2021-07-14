package io.harness;


import com.google.common.util.concurrent.TimeLimiter;
import com.google.inject.Inject;
import com.google.inject.Injector;
import io.harness.exception.NoResultFoundException;
import io.harness.lock.AcquiredLock;
import io.harness.lock.PersistentLocker;
import io.harness.persistence.HPersistence;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mongodb.morphia.aggregation.AggregationPipeline;
import org.mongodb.morphia.aggregation.Group;
import org.mongodb.morphia.query.Query;
import org.springframework.data.mongodb.core.MongoTemplate;
import software.wings.WingsBaseTest;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.instance.AggregationInfo;
import software.wings.service.impl.instance.DashboardStatisticsServiceImpl;
import software.wings.service.impl.instance.ServiceInstanceCount;
import software.wings.service.impl.instance.TemporaryTestInfo;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.mongodb.client.model.Aggregates.group;
import static io.harness.beans.EnvironmentType.NON_PROD;
import static io.harness.beans.EnvironmentType.PROD;
import static java.util.Collections.emptyList;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.mongodb.morphia.aggregation.Accumulator.accumulator;
import static org.mongodb.morphia.aggregation.Group.grouping;
import static org.mongodb.morphia.aggregation.Projection.projection;
import static org.mongodb.morphia.query.Sort.ascending;
import static org.mongodb.morphia.query.Sort.descending;

public class TemporaryTest extends WingsBaseTest {
    @Mock
    Injector injector;
    @Mock
    ExecutorService executorService;
    @Mock
    PersistentLocker persistentLocker;
    @Mock
    TimeLimiter timeLimiter;
    @Inject
    WingsPersistence wingsPersistence;
    @Inject private HPersistence persistence;

    @Mock private AcquiredLock acquiredLock;
    @Inject
    MongoTemplate mongoTemplate;
    DashboardStatisticsServiceImpl dashboardStatisticsService;

//    @Before
//    public void setup() {
//        initMocks(this);
//        dashboardStatisticsService =
//                new DashboardStatisticsServiceImpl(wingsPersistence.getDatastore(query.getEntityClass()));
//    }

    @Test
    @Owner(developers = OwnerRule.MEENAKSHI)
    public void testGetAppInstanceStatsByService(){
        List <Instance> instances = new ArrayList<>();
        List<TemporaryTestInfo> instanceInfoList = new ArrayList<>();
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(PROD).appId("appId1").serviceName("seviveName1").serviceId("serviceId1").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId1").lastArtifactBuildNum("lastArtifactBuildNum1").lastPipelineExecutionName("lastPipelineExecutionName1").build());
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(PROD).appId("appId1").serviceName("seviveName1").serviceId("serviceId1").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId2").lastArtifactBuildNum("lastArtifactBuildNum1").lastPipelineExecutionName("lastPipelineExecutionName2").build());
        instances.add(Instance.builder().appName("appName1").envId("DEV").envName("DEV").envType(PROD).appId("appId1").serviceName("seviveName1").serviceId("serviceId1").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId3").lastArtifactBuildNum("lastArtifactBuildNum2").lastPipelineExecutionName("lastPipelineExecutionName3").build());
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(PROD).appId("appId1").serviceName("seviveName1").serviceId("serviceId1").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId4").lastArtifactBuildNum("lastArtifactBuildNum1").lastPipelineExecutionName("lastPipelineExecutionName4").build());
        instances.add(Instance.builder().appName("appName1").envId("DEV").envName("DEV").envType(PROD).appId("appId1").serviceName("seviveName1").serviceId("serviceId1").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId5").lastArtifactBuildNum("lastArtifactBuildNum3").lastPipelineExecutionName("lastPipelineExecutionName5").build());
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(NON_PROD).appId("appId1").serviceName("seviveName2").serviceId("serviceId2").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId6").lastArtifactBuildNum("lastArtifactBuildNum4").lastPipelineExecutionName("lastPipelineExecutionName6").build());
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(NON_PROD).appId("appId1").serviceName("seviveName2").serviceId("serviceId2").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId7").lastArtifactBuildNum("lastArtifactBuildNum4").lastPipelineExecutionName("lastPipelineExecutionName7").build());
        instances.add(Instance.builder().appName("appName1").envId("DEV").envName("DEV").envType(PROD).appId("appId1").serviceName("seviveName2").serviceId("serviceId2").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId8").lastArtifactBuildNum("lastArtifactBuildNum5").lastPipelineExecutionName("lastPipelineExecutionName8").build());
        instances.add(Instance.builder().appName("appName1").envId("QA").envName("QA").envType(PROD).appId("appId1").serviceName("seviveName2").serviceId("serviceId2").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId9").lastArtifactBuildNum("lastArtifactBuildNum4").lastPipelineExecutionName("lastPipelineExecutionName9").build());
        instances.add(Instance.builder().appName("appName1").envId("DEV").envName("DEV").envType(PROD).appId("appId1").serviceName("seviveName2").serviceId("serviceId2").infraMappingType("AWS_SSH").lastArtifactId("lastArtifactId10").lastArtifactBuildNum("lastArtifactBuildNum6").lastPipelineExecutionName("lastPipelineExecutionName10").build());

        String QA="QA";
        String DEV = "DEV";
        wingsPersistence.save(instances);
        Query<Instance> query = wingsPersistence.createQuery(Instance.class);
        query.and(query.criteria("appId").equal("appId1"),
                query.or(query.criteria("envId").equal(QA), query.criteria("envId").equal(DEV)));
        AggregationPipeline aggregationPipeline =
                wingsPersistence.getDatastore(query.getEntityClass())
                .createAggregation(Instance.class)
                .match(query)
                .group(Group.id(grouping("serviceId")),
//                        grouping("count", accumulator("$sum", 1)),
//                        grouping("appInfo", grouping("$first", projection("id", "appId"), projection("name", "appName"))),
                        grouping(
                                "serviceInfo", grouping("$first", projection("id", "serviceId"), projection("name", "serviceName"))),
                        grouping("envInfo",
                                grouping(
                                        "$first", projection("id", "envId"), projection("name", "envName"), projection("type", "envType")))
                        ,grouping("ServiceInfoSummary", grouping("$push", projection("id", "serviceId"), projection("envId", "envId"), projection("lastArtifactBuildNum", "lastArtifactBuildNum"))));
//                        grouping("artifactInfo",
//                                grouping("$first", projection("id", "lastArtifactId"), projection("name", "lastArtifactName"),
//                                        projection("buildNo", "lastArtifactBuildNum"), projection("streamId", "lastArtifactStreamId"),
//                                        projection("deployedAt", "lastDeployedAt"), projection("sourceName", "lastArtifactSourceName"))),
//                        grouping(
//                                "instanceInfoList", grouping("$addToSet", projection("id", "_id"), projection("name", "hostName"))))
//                .sort(ascending("_id.serviceId"), ascending("_id.envId"), descending("count"))
//                .aggregate(AggregationInfo.class)
//                .forEachRemaining(instanceInfo -> {
////                    instanceInfoList.add(instanceInfo);
////                    log.info(instanceInfo.toString());
       //         });

        final Iterator<TemporaryTestInfo> aggregate =
                HPersistence.retry(() -> aggregationPipeline.aggregate(TemporaryTestInfo.class));
        aggregate.forEachRemaining(instanceInfoList::add);
        System.out.println(instanceInfoList);




    }

}
