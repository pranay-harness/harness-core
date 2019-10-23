package software.wings.service.impl;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static software.wings.infra.InfraDefinitionTestConstants.RESOURCE_CONSTRAINT_NAME;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.inject.Inject;

import com.mongodb.DuplicateKeyException;
import io.harness.category.element.UnitTests;
import io.harness.distribution.constraint.Constraint.Strategy;
import io.harness.distribution.constraint.Consumer.State;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ResourceConstraint;
import software.wings.beans.ResourceConstraint.ResourceConstraintKeys;
import software.wings.beans.ResourceConstraintInstance;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceBuilder;
import software.wings.beans.ResourceConstraintInstance.ResourceConstraintInstanceKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ResourceConstraintService;
import software.wings.sm.states.HoldingScope;

import java.util.List;

public class ResourceConstraintServiceImplTest extends WingsBaseTest {
  private static final String RESOURCE_CONSTRAINT_ID = "RC_ID";
  private static final String WORKFLOW_ID_1 = "WF_ID_1";
  private static final String WORKFLOW_ID_2 = "WF_ID_2";

  @Mock private WingsPersistence wingsPersistence;
  @Mock private Query query;
  @Mock private FieldEnd fieldEnd;

  @InjectMocks @Inject private ResourceConstraintService resourceConstraintService;

  private final ResourceConstraint resourceConstraint = ResourceConstraint.builder()
                                                            .name(RESOURCE_CONSTRAINT_NAME)
                                                            .accountId(ACCOUNT_ID)
                                                            .capacity(1)
                                                            .strategy(Strategy.FIFO)
                                                            .build();

  private final ResourceConstraintInstanceBuilder instanceBuilder = ResourceConstraintInstance.builder()
                                                                        .uuid(generateUuid())
                                                                        .appId(APP_ID)
                                                                        .resourceConstraintId(RESOURCE_CONSTRAINT_ID)
                                                                        .acquiredAt(System.currentTimeMillis())
                                                                        .permits(4);

  private ResourceConstraintInstance instance1 = instanceBuilder.releaseEntityId(WORKFLOW_ID_1)
                                                     .state(State.ACTIVE.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .build();

  private ResourceConstraintInstance instance2 = instanceBuilder.releaseEntityId(WORKFLOW_ID_2)
                                                     .state(State.BLOCKED.name())
                                                     .resourceUnit(INFRA_MAPPING_ID)
                                                     .releaseEntityType(HoldingScope.WORKFLOW.name())
                                                     .build();

  @Test
  @Category(UnitTests.class)
  public void ensureResourceConstraintForInfrastructureThrottlingWhenExists() {
    doReturn(query).when(wingsPersistence).createQuery(eq(ResourceConstraint.class));
    doReturn(query).doReturn(query).when(query).filter(ResourceConstraintKeys.accountId, ACCOUNT_ID);
    doReturn(query).when(query).filter(ResourceConstraintKeys.name, "Queuing");
    doReturn(resourceConstraint).when(query).get();
    doThrow(DuplicateKeyException.class).when(wingsPersistence).save(any(ResourceConstraint.class));
    ResourceConstraint savedResourceConstraint =
        resourceConstraintService.ensureResourceConstraintForConcurrency(ACCOUNT_ID, "Queuing");

    assertThat(savedResourceConstraint.getUuid()).isEqualTo(resourceConstraint.getUuid());
    assertThat(savedResourceConstraint.getName()).isEqualTo(resourceConstraint.getName());
  }

  @Test
  @Category(UnitTests.class)
  public void ensureResourceConstraintForInfrastructureThrottlingWhenDoNotExists() {
    doReturn(query).when(wingsPersistence).createQuery(eq(ResourceConstraint.class));
    doReturn(query).doReturn(query).when(query).filter(ResourceConstraintKeys.accountId, ACCOUNT_ID);
    doReturn(query).when(query).filter(ResourceConstraintKeys.name, "Queuing");
    doReturn(resourceConstraint).when(query).get();
    doThrow(DuplicateKeyException.class).when(wingsPersistence).save(resourceConstraint);
    ResourceConstraint savedResourceConstraint =
        resourceConstraintService.ensureResourceConstraintForConcurrency(ACCOUNT_ID, "Queuing");

    assertThat(savedResourceConstraint.getUuid()).isEqualTo(resourceConstraint.getUuid());
    assertThat(savedResourceConstraint.getName()).isEqualTo(resourceConstraint.getName());
  }

  @Test
  @Category(UnitTests.class)
  public void shouldTestFetchEntityIdListForUnitAndEntityType() {
    doReturn(query).when(wingsPersistence).createQuery(eq(ResourceConstraintInstance.class));
    doReturn(query).doReturn(query).when(query).filter(ResourceConstraintInstanceKeys.appId, APP_ID);
    doReturn(query).when(query).filter(ResourceConstraintInstanceKeys.resourceUnit, INFRA_MAPPING_ID);
    doReturn(query).doReturn(query).when(query).filter(
        ResourceConstraintInstanceKeys.releaseEntityType, HoldingScope.WORKFLOW.name());
    doReturn(fieldEnd).when(query).field(ResourceConstraintInstanceKeys.state);
    doReturn(query).when(fieldEnd).in(asList(State.ACTIVE.name(), State.BLOCKED.name()));
    doReturn(asList(instance1, instance2)).when(query).asList();
    List<ResourceConstraintInstance> entityIds =
        resourceConstraintService.fetchResourceConstraintInstancesForUnitAndEntityType(
            APP_ID, INFRA_MAPPING_ID, HoldingScope.WORKFLOW.name());

    assertThat(entityIds).isNotNull().hasSize(2);
  }
}
