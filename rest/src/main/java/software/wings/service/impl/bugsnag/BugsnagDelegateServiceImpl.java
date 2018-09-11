package software.wings.service.impl.bugsnag;

import com.google.common.collect.HashBiMap;
import com.google.inject.Inject;

import io.harness.network.Http;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;
import software.wings.beans.BugsnagConfig;
import software.wings.helpers.ext.apm.APMRestClient;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.ThirdPartyApiCallLog;
import software.wings.service.intfc.security.EncryptionService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

/**
 * Created by Praveen
 */
public class BugsnagDelegateServiceImpl implements BugsnagDelegateService {
  private static final String organizationsUrl = "user/organizations";
  private static final String projectsUrl = "organizations/orgId/projects";
  private static final Logger logger = LoggerFactory.getLogger(BugsnagDelegateServiceImpl.class);
  @Inject private EncryptionService encryptionService;

  public Set<BugsnagApplication> getOrganizations(
      BugsnagConfig config, List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog) {
    try {
      Map<String, Object> hMap = HashBiMap.create();
      APMRestClient client = getAPMRestClient(config, encryptedDataDetails);
      for (Map.Entry<String, String> entry : config.headersMap().entrySet()) {
        hMap.put(entry.getKey(), entry.getValue());
      }
      Call<Object> request = client.collect(organizationsUrl, hMap, HashBiMap.create());
      Response<Object> response = request.execute();
      List<Map<String, String>> orgs = (List<Map<String, String>>) response.body();
      List<BugsnagApplication> returnList = new ArrayList<>();
      for (Map<String, String> org : orgs) {
        returnList.add(BugsnagApplication.builder().name(org.get("name")).id(org.get("id")).build());
      }
      Set<BugsnagApplication> sorted = new TreeSet<>();
      sorted.addAll(returnList);
      return sorted;
    } catch (Exception ex) {
      logger.error("Exception while fetching organizations from bugsnag");
    }
    return null;
  }
  public Set<BugsnagApplication> getProjects(BugsnagConfig config, String orgId,
      List<EncryptedDataDetail> encryptedDataDetails, ThirdPartyApiCallLog apiCallLog) {
    try {
      Map<String, Object> hMap = HashBiMap.create();
      APMRestClient client = getAPMRestClient(config, encryptedDataDetails);
      for (Map.Entry<String, String> entry : config.headersMap().entrySet()) {
        hMap.put(entry.getKey(), entry.getValue());
      }
      String url = projectsUrl.replaceAll("orgId", orgId);
      Call<Object> request = client.collect(url, hMap, HashBiMap.create());
      Response<Object> response = request.execute();
      List<Map<String, String>> projects = (List<Map<String, String>>) response.body();
      List<BugsnagApplication> returnList = new ArrayList<>();
      for (Map<String, String> org : projects) {
        returnList.add(BugsnagApplication.builder().name(org.get("name")).id(org.get("id")).build());
      }
      Set<BugsnagApplication> sorted = new TreeSet<>();
      sorted.addAll(returnList);
      return sorted;
    } catch (Exception ex) {
      logger.error("Exception while fetching projects from bugsnag");
    }
    return null;
  }

  private APMRestClient getAPMRestClient(final BugsnagConfig config, List<EncryptedDataDetail> encryptedDataDetails) {
    encryptionService.decrypt(config, encryptedDataDetails);
    final Retrofit retrofit =
        new Retrofit.Builder()
            .baseUrl(config.getUrl())
            .addConverterFactory(JacksonConverterFactory.create())
            .client(
                Http.getOkHttpClientWithNoProxyValueSet(config.getUrl()).connectTimeout(30, TimeUnit.SECONDS).build())
            .build();
    return retrofit.create(APMRestClient.class);
  }
}
