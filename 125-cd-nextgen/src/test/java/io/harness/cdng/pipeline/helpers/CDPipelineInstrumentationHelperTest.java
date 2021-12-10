package io.harness.cdng.pipeline.helpers;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.dtos.InstanceDTO;
import io.harness.rule.Owner;
import io.harness.service.instance.InstanceService;
import org.jooq.Row3;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.List;

import static io.harness.rule.OwnerRule.MLUKIC;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OwnedBy(HarnessTeam.CDP)
public class CDPipelineInstrumentationHelperTest extends CategoryTest {

    @Mock private InstanceService instanceService;
    @InjectMocks private CDPipelineInstrumentationHelper cdPipelineInstrumentationHelper;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    @Owner(developers = MLUKIC)
    @Category(UnitTests.class)
    public void testGetServiceInstancesInIntervalException() {
        String accountId = "TestAccountId";
        long startInterval = -10;
        long endInterval = 0;

        doThrow(new RuntimeException("Some problem"))
                .when(instanceService)
                .getInstancesDeployedInInterval(any(), anyLong(), anyLong());

        List<InstanceDTO> serviceInstances = cdPipelineInstrumentationHelper.getServiceInstancesInInterval(accountId, startInterval, endInterval);

        assertThat(serviceInstances).isNotNull();
        assertThat(serviceInstances.size()).isEqualTo(0);
    }

    @Test
    @Owner(developers = MLUKIC)
    @Category(UnitTests.class)
    public void testHappyPath() {
        String accountId = "TestAccountId";
        long startInterval = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        long endInterval = System.currentTimeMillis();

        InstanceDTO instanceDTO1 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj1")
                .orgIdentifier("org")
                .serviceIdentifier("svcId1")
                .build();

        InstanceDTO instanceDTO2 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj2")
                .orgIdentifier("org")
                .serviceIdentifier("svcId2")
                .build();

        InstanceDTO instanceDTO3 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj2")
                .orgIdentifier("org")
                .serviceIdentifier("svcId3")
                .build();

        InstanceDTO instanceDTO4 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj1")
                .orgIdentifier("org")
                .serviceIdentifier("svcId1")
                .build();

        List<InstanceDTO> responseList = new ArrayList<>();
        responseList.add(instanceDTO1);
        responseList.add(instanceDTO2);
        responseList.add(instanceDTO3);
        responseList.add(instanceDTO4);

        doReturn(responseList)
                .when(instanceService)
                .getInstancesDeployedInInterval(any(), anyLong(), anyLong());

        List<InstanceDTO> serviceInstances = cdPipelineInstrumentationHelper.getServiceInstancesInInterval(accountId, startInterval, endInterval);
        long activeServicesCount = cdPipelineInstrumentationHelper.getTotalNumberOfActiveServices(serviceInstances);

        assertThat(serviceInstances).isNotNull();
        assertThat(serviceInstances.size()).isEqualTo(4);
        assertThat(activeServicesCount).isEqualTo(3);
    }

    @Test
    @Owner(developers = MLUKIC)
    @Category(UnitTests.class)
    public void testGetOrgProjectServiceRowsException() {
        String accountId = "TestAccountId";
        long startInterval = System.currentTimeMillis() - 24 * 60 * 60 * 1000;
        long endInterval = System.currentTimeMillis();

        InstanceDTO instanceDTO1 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj1")
                .orgIdentifier("org")
                .serviceIdentifier("svcId1")
                .build();

        InstanceDTO instanceDTO2 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj2")
                .orgIdentifier("org")
                .serviceIdentifier("svcId2")
                .build();

        InstanceDTO instanceDTO3 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj2")
                .orgIdentifier("org")
                .serviceIdentifier("svcId3")
                .build();

        InstanceDTO instanceDTO4 = InstanceDTO.builder()
                .accountIdentifier(accountId)
                .projectIdentifier("proj1")
                .orgIdentifier("org")
                .serviceIdentifier("svcId1")
                .build();

        List<InstanceDTO> serviceInstanceList = new ArrayList<>();
        serviceInstanceList.add(instanceDTO1);
        serviceInstanceList.add(instanceDTO2);
        serviceInstanceList.add(instanceDTO3);
        serviceInstanceList.add(instanceDTO4);

        Row3<String, String, String>[] rows = cdPipelineInstrumentationHelper.getOrgProjectServiceRows(null);

        assertThat(rows.length).isEqualTo(0);
    }
}
