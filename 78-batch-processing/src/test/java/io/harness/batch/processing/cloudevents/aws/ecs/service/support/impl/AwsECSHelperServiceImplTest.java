package io.harness.batch.processing.cloudevents.aws.ecs.service.support.impl;

import static io.harness.rule.OwnerRule.HITESH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import com.google.common.collect.ImmutableList;

import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.AmazonECSException;
import com.amazonaws.services.ecs.model.ListClustersRequest;
import com.amazonaws.services.ecs.model.ListClustersResult;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

import java.util.List;

public class AwsECSHelperServiceImplTest extends CategoryTest {
  @Spy private AwsECSHelperServiceImpl awsECSHelperService;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListEKSClusters() {
    String nextToken = "nextClusterToken";
    AmazonECSClient mockClient = mock(AmazonECSClient.class);
    doReturn(mockClient).when(awsECSHelperService).getAmazonECSClient(any(), any());
    doReturn(
        new ListClustersResult().withClusterArns(ImmutableList.of("cluster1", "cluster2")).withNextToken(nextToken))
        .when(mockClient)
        .listClusters(new ListClustersRequest().withNextToken(null));

    doReturn(new ListClustersResult().withClusterArns(ImmutableList.of("cluster3", "cluster4")).withNextToken(null))
        .when(mockClient)
        .listClusters(new ListClustersRequest().withNextToken(nextToken));

    List<String> eksClusters = awsECSHelperService.listECSClusters(any(), any());
    assertThat(eksClusters).hasSize(4);
    assertThat(eksClusters.get(0)).isEqualTo("cluster1");
    assertThat(eksClusters.get(1)).isEqualTo("cluster2");
    assertThat(eksClusters.get(2)).isEqualTo("cluster3");
    assertThat(eksClusters.get(3)).isEqualTo("cluster4");
  }

  @Test
  @Owner(developers = HITESH)
  @Category(UnitTests.class)
  public void testListEmptyEKSClustersWhenExceptionOccur() {
    AmazonECSClient mockClient = mock(AmazonECSClient.class);
    doReturn(mockClient).when(awsECSHelperService).getAmazonECSClient(any(), any());

    doThrow(new AmazonECSException("ECS Exception"))
        .when(mockClient)
        .listClusters(new ListClustersRequest().withNextToken(null));
    List<String> eksClusters = awsECSHelperService.listECSClusters(any(), any());
    assertThat(eksClusters).hasSize(0);
  }
}
