package software.wings.integration.migration.legacy;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.sm.StateMachine;

@Integration
public class StateMachineProjectionTest extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void testSmProjection() {
    String stateMachineId = "GyWBUWCISwSWjG2WYTXzBQ";

    StateMachine stateMachine = wingsPersistence.createQuery(StateMachine.class)
                                    .field(ID_KEY)
                                    .equal(stateMachineId)
                                    .field("appId")
                                    .equal("Or2FWFL6TguB964BEiiD2A")
                                    .project("initialStateName", true)
                                    .project("initialStateName", true)
                                    .get();

    System.out.println(stateMachine);
  }
}
