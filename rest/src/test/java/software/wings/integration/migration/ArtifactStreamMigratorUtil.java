package software.wings.integration.migration;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.SortOrder.OrderType;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.List;

/**
 * @author rktummala on 11/30/17
 */
@Integration
@Ignore
public class ArtifactStreamMigratorUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void setNameFieldInArtifactStream() {
    PageRequest<ArtifactStream> pageRequest =
        aPageRequest().withLimit(UNLIMITED).addOrder("serviceId", OrderType.ASC).build();
    System.out.println("Retrieving artifact streams");
    PageResponse<ArtifactStream> pageResponse = wingsPersistence.query(ArtifactStream.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No artifact streams found");
      return;
    }

    updateArtifactStreams(pageResponse.getResponse());
  }

  private void updateArtifactStreams(List<ArtifactStream> artifactStreamList) {
    int counter = 0;
    String prevServiceId = null;
    for (ArtifactStream artifactStream : artifactStreamList) {
      // First find out any artifact stream exists with the same name for the same service.
      PageRequest<ArtifactStream> pageRequest = aPageRequest()
                                                    .addFilter("appId", Operator.EQ, artifactStream.getAppId())
                                                    .addFilter("serviceId", Operator.EQ, artifactStream.getServiceId())
                                                    .addFilter("name", Operator.EQ, artifactStream.generateName())
                                                    .build();
      if (!artifactStream.getServiceId().equals(prevServiceId)) {
        counter = 0;
        prevServiceId = artifactStream.getServiceId();
      }

      System.out.println("Checking if artifact stream info with name exists: " + artifactStream.generateName());
      PageResponse<ArtifactStream> response = wingsPersistence.query(ArtifactStream.class, pageRequest);
      String name = artifactStream.generateName();
      if (!response.isEmpty()) {
        name = name + "." + ++counter;
      }

      wingsPersistence.updateField(ArtifactStream.class, artifactStream.getUuid(), "name", name);
    }
  }
}
