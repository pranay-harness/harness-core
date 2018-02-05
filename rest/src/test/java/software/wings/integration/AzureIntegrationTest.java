package software.wings.integration;

import org.junit.Ignore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.rules.Integration;
import software.wings.service.impl.AzureHelperService;

@Integration
@Ignore
/*
This test class is making calls to Azure. The purpose is to do directed testing for Azure APIs.
This is not to be run as an automated test[hence the @Ignore].
 */
public class AzureIntegrationTest {
  private static final Logger logger = LoggerFactory.getLogger(AzureIntegrationTest.class);

  private static final String clientId = "a19170f2-a2b1-49d2-9a0f-091b02fb7cb7";
  private static final String tenantId = "bd57732b-0443-4542-887b-e601cff640a1";
  private static final String key = "aqFVAJLuVL7fyOyv0OPW6N8HTyQWr1366t1smLIDfXA=";
  private static final String subscriptionId = "12d2db62-5aa9-471d-84bb-faa489b3e319";

  public static void main(String[] args) {
    AzureAuthenticationTest();
  }

  private static void AzureAuthenticationTest() {
    logger.info("AzureAuthenticationTest: calling Azure.");

    AzureHelperService azure = new AzureHelperService();
    azure.validateAzureAccountCredential(clientId, tenantId, key);

    logger.info("AzureAuthenticationTest: Done.");
  }
}
