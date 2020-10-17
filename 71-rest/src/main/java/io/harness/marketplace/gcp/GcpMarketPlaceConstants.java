package io.harness.marketplace.gcp;

public interface GcpMarketPlaceConstants {
  String SERVICE_ACCOUNT_INTEGRATION_PATH = "/opt/harness/svc/gcp_marketplace_creds.json";
  String PROJECT_ID = "harness-public";
  String APPROVAL_SIGNUP_NAME = "signup";
  String GCP_METRIC_NAME =
      "harness-continuous-delivery.gcpmarketplace.harness.io/harness_pro_additional_service_instances";
  String GCP_OPERATION_NAME = "GCP Instance Usage Report";
  String HARNESS_GCP_APPLICATION = "HARNESS_INSTANCE_USAGE_REPORTING_APP";
  String SERVICE_NAME = "harness-continuous-delivery.gcpmarketplace.harness.io";
  String SERVICE_CONTROL_API_END_POINT = "https://servicecontrol.googleapis.com";
  String ENTITLEMENT_ACTIVATED = "ENTITLEMENT_ACTIVE";
  String TOKEN_ISSUER =
      "https://www.googleapis.com/robot/v1/metadata/x509/cloud-commerce-partner@system.gserviceaccount.com";
  String TOKEN_AUDIENCE = "app.harness.io";
  int DEFAULT_LICENCE_UNITS = 50;
}
