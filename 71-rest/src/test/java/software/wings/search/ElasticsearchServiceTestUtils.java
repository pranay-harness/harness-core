package software.wings.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.search.TotalHits;
import org.apache.lucene.search.TotalHits.Relation;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponse.Clusters;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.internal.InternalSearchResponse;
import software.wings.beans.EntityType;
import software.wings.search.entities.application.ApplicationView;
import software.wings.search.entities.deployment.DeploymentView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

public class ElasticsearchServiceTestUtils {
  static float score = 0.2345f;

  public static ApplicationView createApplicationView() {
    ApplicationView applicationView = new ApplicationView();
    applicationView.setId("appId");
    applicationView.setName("appName");
    applicationView.setType(EntityType.APPLICATION);
    applicationView.setCreatedAt(System.currentTimeMillis());
    return applicationView;
  }

  public static DeploymentView createDeploymentView() {
    DeploymentView deploymentView = new DeploymentView();
    deploymentView.setId("deploymentId" + System.currentTimeMillis());
    deploymentView.setName("name");
    deploymentView.setCreatedAt(System.currentTimeMillis());
    deploymentView.setType(EntityType.DEPLOYMENT);
    deploymentView.setWorkflowInPipeline(false);
    return deploymentView;
  }

  public static byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

  public static SearchResponse getDeploymentSearchResponse() {
    ShardSearchFailure[] shardFailures = new ShardSearchFailure[0];
    try {
      ObjectMapper mapper = new ObjectMapper();

      DeploymentView deploymentView = createDeploymentView();
      BytesReference source = new BytesArray(mapper.writeValueAsBytes(deploymentView));
      SearchHit searchHit = new SearchHit(1);
      searchHit.score(score);
      searchHit.sourceRef(source);

      DeploymentView deploymentView1 = createDeploymentView();
      BytesReference source1 = new BytesArray(mapper.writeValueAsBytes(deploymentView1));
      SearchHit searchHit1 = new SearchHit(2);
      searchHit1.score(score);
      searchHit1.sourceRef(source1);

      SearchHit[] hits = new SearchHit[] {searchHit, searchHit1};
      SearchHits searchHits = new SearchHits(hits, new TotalHits(2, Relation.GREATER_THAN_OR_EQUAL_TO), score);
      InternalSearchResponse internalSearchResponse =
          new InternalSearchResponse(searchHits, null, null, null, false, false, 1);

      return new SearchResponse(
          internalSearchResponse, "scrollId", 1, 1, 0, 10000, shardFailures, new Clusters(1, 1, 0));

    } catch (IOException e) {
      return null;
    }
  }
}
