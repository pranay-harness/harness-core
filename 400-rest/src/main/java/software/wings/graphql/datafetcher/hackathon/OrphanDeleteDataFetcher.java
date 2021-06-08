package software.wings.graphql.datafetcher.hackathon;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.*;

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
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.inject.Inject;
import java.io.IOException;
import java.security.GeneralSecurityException;
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
    log.info("Orphan Delete query: {}", query);
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

    try {
      Compute computeService = createComputeService();
      Compute.Disks.Delete request = computeService.disks().delete(project, zone, resourceName);
      request.execute();
    } catch (Exception e) {
      log.error("Exception {}", e);
    }
  }

  public static Compute createComputeService() throws IOException, GeneralSecurityException {
    HttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
    JsonFactory jsonFactory = JacksonFactory.getDefaultInstance();

    ServiceAccountCredentials serviceAccountCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    GoogleCredential googleCredential = toGoogleCredential(serviceAccountCredentials);
    return new Compute.Builder(httpTransport, jsonFactory, googleCredential)
        .setApplicationName("Google-ComputeSample/0.1")
        .build();
  }
}
