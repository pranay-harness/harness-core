package io.harness.cvng.core.services;

public interface CVNextGenConstants {
  String CV_NEXTGEN_RESOURCE_PREFIX = "cv-nextgen";
  String APPD_TIER_ID_PLACEHOLDER = "__tier_name__";
  String APPD_METRIC_DATA_NOT_FOUND = "METRIC DATA NOT FOUND";
  String DELEGATE_DATA_COLLETION = "delegate-data-collection";
  String DELEGATE_DATA_COLLETION_TASK = "delegate-data-collection-task";
  String VERIFICATION_SERVICE_SECRET = "VERIFICATION_SERVICE_SECRET";
  String CV_NEXT_GEN_SERVICE_ENDPOINTS_PREFIX = CV_NEXTGEN_RESOURCE_PREFIX + "/service/";
  String CV_DATA_COLLECTION_PATH = CV_NEXT_GEN_SERVICE_ENDPOINTS_PREFIX + "/cv-data-collection-task";
}
