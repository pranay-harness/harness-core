package io.harness.batch.processing.dummydata;

import static io.harness.rule.OwnerRule.ROHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.CategoryTest;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.instance.HarnessServiceInfo;

import java.util.Arrays;
import java.util.List;

public class K8sDummyInstanceDataGeneratorTest extends CategoryTest {
  @Inject @InjectMocks K8sDummyInstanceDataGenerator k8sDummyInstanceDataGenerator;
  @Mock InstanceDataDao instanceDataDao;

  private static String APP_ID = "appId";
  private static String CLOUD_PROVIDER_ID = "cloudProviderId";
  private static String ENV_ID = "envId";
  private static String INFRA_MAPPING_ID = "infraMappingId";
  private static String SERVICE_ID = "serviceId";

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    when(instanceDataDao.create(any())).thenReturn(true);
  }

  @Test
  @Owner(developers = ROHIT)
  @Category(UnitTests.class)
  public void shouldWriteK8sUtilizationMetrics() {
    int numberOfCluster = 2;
    int numberOfDays = 25;
    long ONE_DAY_MILLIS = 86400000;
    List<Integer> listOfNumberOfNodesInEachCluster = Arrays.asList(2, 2);
    List<List<Integer>> listOfNumberOfPodsInEachNode = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(2, 4));
    List<HarnessServiceInfo> harnessServiceInfoList =
        Arrays.asList(new HarnessServiceInfo(SERVICE_ID, APP_ID, CLOUD_PROVIDER_ID, ENV_ID, INFRA_MAPPING_ID));
    long startTime = 15400000000L;
    long endTime = startTime + (numberOfDays * ONE_DAY_MILLIS);
    boolean isCreatedAndInsertedIntoDB = k8sDummyInstanceDataGenerator.createAndInsertDummyData(numberOfCluster,
        listOfNumberOfNodesInEachCluster, listOfNumberOfPodsInEachNode, startTime, endTime, harnessServiceInfoList);
    assertThat(isCreatedAndInsertedIntoDB).isTrue();
  }
}
