package software.wings.graphql.datafetcher.hackathon;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static software.wings.graphql.datafetcher.billing.CloudBillingHelper.recommendations;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.bigquery.BigQueryService;

import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.datafetcher.billing.CloudBillingHelper;
import software.wings.security.PermissionAttribute.PermissionType;
import software.wings.security.annotations.AuthRule;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.compute.Compute;
import com.google.api.services.compute.model.Disk;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class OrphanDeleteDataFetcher
    extends AbstractObjectDataFetcher<QLRecommendationsDeletionData, QLRecommendationsDeletionParameters> {
  @Inject CloudBillingHelper cloudBillingHelper;
  @Inject BigQueryService bigQueryService;

  private static final String DELETE_QUERY = "DELETE FROM %s WHERE resource = '%s'";

  @Override
  @AuthRule(permissionType = PermissionType.LOGGED_IN)
  protected QLRecommendationsDeletionData fetch(QLRecommendationsDeletionParameters parameters, String accountId) {
    try {
      deleteResource(parameters.getResourceName());
    } catch (Exception e) {
      return QLRecommendationsDeletionData.builder().isDeleted(false).build();
    }

    String cloudProviderTableName = cloudBillingHelper.getCloudProviderTableName(accountId, recommendations);
    BigQuery bigQuery = bigQueryService.get();
    String query = String.format(DELETE_QUERY, cloudProviderTableName, parameters.getResourceName());
    QueryJobConfiguration queryConfig = QueryJobConfiguration.newBuilder(query).build();
    log.info("Orphan overview query: {}", query);
    try {
      bigQuery.query(queryConfig);
    } catch (InterruptedException e) {
      log.error("Failed to delete Orphan resource. {}", e.toString());
      Thread.currentThread().interrupt();
      return null;
    }
    return QLRecommendationsDeletionData.builder().isDeleted(true).build();
  }

  private void deleteResource(String resourceName) throws IOException, GeneralSecurityException {
    String project = "ccm-play";
    String zone = "us-central1-c";

    Compute computeService = createComputeService();
    //    Compute.Disks.Delete request = computeService.disks().delete(project, zone, disk);
    //    Operation response = request.execute();

    Compute.Disks.Get getRequest = computeService.disks().get(project, zone, resourceName);
    Disk diskResponse = getRequest.execute();
    log.info("Get Call Disk Response {}", diskResponse);
  }

  public static Compute createComputeService() throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    GoogleCredential credential = GoogleCredential.getApplicationDefault();
    if (credential.createScopedRequired()) {
      credential = credential.createScoped(Arrays.asList("https://www.googleapis.com/auth/cloud-platform"));
    }

    return new Compute.Builder(httpTransport, jsonFactory, credential)
        .setApplicationName("Google-ComputeSample/0.1")
        .build();
  }
}
