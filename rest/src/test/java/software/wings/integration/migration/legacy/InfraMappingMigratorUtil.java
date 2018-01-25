package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * @author rktummala on 10/24/17
 */
@Integration
@Ignore
public class InfraMappingMigratorUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void setNameFieldInInfraMapping() {
    PageRequest<InfrastructureMapping> pageRequest =
        aPageRequest().withLimit(UNLIMITED).addOrder("envId", OrderType.ASC).build();
    System.out.println("Retrieving infra mapping info");
    PageResponse<InfrastructureMapping> pageResponse = wingsPersistence.query(InfrastructureMapping.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No infra mapping info found");
      return;
    }

    updateInfraMappings(pageResponse.getResponse());
  }

  private void updateInfraMappings(List<InfrastructureMapping> infraMappingList) {
    int counter = 0;
    String prevEnvId = null;
    for (InfrastructureMapping infraMapping : infraMappingList) {
      // First find out any infra mapping exists with the same name for the same env.
      PageRequest<InfrastructureMapping> pageRequest =
          aPageRequest()
              .addFilter("appId", Operator.EQ, infraMapping.getAppId())
              .addFilter("envId", Operator.EQ, infraMapping.getEnvId())
              .addFilter("name", Operator.EQ, infraMapping.getDefaultName())
              .build();
      if (!infraMapping.getEnvId().equals(prevEnvId)) {
        counter = 0;
        prevEnvId = infraMapping.getEnvId();
      }

      System.out.println("Checking if infra mapping info with name exists: " + infraMapping.getDefaultName());
      PageResponse<InfrastructureMapping> response = wingsPersistence.query(InfrastructureMapping.class, pageRequest);
      String name = infraMapping.getDefaultName();
      if (!response.isEmpty()) {
        name = name + "." + ++counter;
      }

      wingsPersistence.updateField(InfrastructureMapping.class, infraMapping.getUuid(), "name", name);
    }
  }
}
