package software.wings.integration.service;

import software.wings.WingsBaseTest;
import software.wings.rules.Integration;

@Integration
public class ResourceConstraintServiceIntegrationTest extends WingsBaseTest {
  /*@Inject private OwnerManager ownersManager;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;

  @Inject private ResourceConstraintService resourceConstraintService;

  private Seed seed;
  private Owners owners;

  @Before
  public void setUp() {
    seed = Randomizer.seed();
    owners = ownersManager.create();
  }

  @Test
  public void testOrdering() throws InvalidPermitsException, UnableToRegisterConsumerException {
    final ResourceConstraint resourceConstraint = resourceConstraintGenerator.ensureResourceConstraint(
        seed, owners, ResourceConstraint.builder().uuid(generateUuid()).build());
    final Constraint constraint = resourceConstraintService.createAbstraction(resourceConstraint);

    Concurrent.test(10, i -> {
      for (int j = 0; j < 10; j++) {
        Map<String, Object> context = new HashMap();
        context.put(ResourceConstraintInstance.APP_ID_KEY, ResourceConstraintInstance.GLOBAL_APP_ID);
        context.put(ResourceConstraintInstance.RELEASE_ENTITY_TYPE_KEY, "WORKFLOW");
        context.put(ResourceConstraintInstance.RELEASE_ENTITY_ID_KEY, generateUuid());
        context.put(ResourceConstraintInstance.ORDER_KEY,
            resourceConstraintService.getMaxOrder(resourceConstraint.getUuid()) + 1);

        try {
          constraint.registerConsumer(
              new ConsumerId("consumer" + i + "_" + j), 1, context, resourceConstraintService.getRegistry());
        } catch (ConstraintException e) {
          throw new RuntimeException(e);
        }
      }
    });

    assertThat(resourceConstraintService.getMaxOrder(resourceConstraint.getUuid())).isEqualTo(100);
  }*/
}
