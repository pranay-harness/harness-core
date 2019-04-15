package io.harness.delegate.task.mixin;

import static org.apache.commons.lang3.StringUtils.isBlank;

import com.amazonaws.regions.Regions;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.expression.DummySubstitutor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class HttpConnectionExecutionCapabilityGenerator {
  private static final Map<Regions, String> AWS_REGION_URL_MAP = new ConcurrentHashMap<>();
  static {
    // See AWS doc https://docs.aws.amazon.com/general/latest/gr/rande.html
    AWS_REGION_URL_MAP.put(Regions.US_EAST_1, "https://apigateway.us-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_EAST_2, "https://apigateway.us-east-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_WEST_1, "https://apigateway.us-west-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_WEST_2, "https://apigateway.us-west-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTH_1, "https://apigateway.ap-south-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_1, "https://apigateway.ap-northeast-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_NORTHEAST_2, "https://apigateway.ap-northeast-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_1, "https://apigateway.ap-southeast-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.AP_SOUTHEAST_2, "https://apigateway.ap-southeast-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.CA_CENTRAL_1, "https://apigateway.ca-central-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.CN_NORTH_1, "https://apigateway.cn-north-1.amazonaws.com.cn");
    AWS_REGION_URL_MAP.put(Regions.CN_NORTHWEST_1, "https://apigateway.cn-northwest-1.amazonaws.com.cn");
    AWS_REGION_URL_MAP.put(Regions.EU_CENTRAL_1, "https://apigateway.eu-central-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_1, "https://apigateway.eu-west-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_2, "https://apigateway.eu-west-2.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.EU_WEST_3, "https://apigateway.eu-west-3.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.SA_EAST_1, "https://apigateway.sa-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.US_GOV_EAST_1, "https://apigateway.us-gov-east-1.amazonaws.com");
    AWS_REGION_URL_MAP.put(Regions.GovCloud, "https://apigateway.us-gov-west-1.amazonaws.com");
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapability(String urlString) {
    try {
      URI uri = new URI(DummySubstitutor.substitute(urlString));

      return HttpConnectionExecutionCapability.builder()
          .scheme(uri.getScheme())
          .hostName(getHostName(uri))
          .port(uri.getPort() == -1 ? null : Integer.toString(uri.getPort()))
          .url(urlString)
          .build();

    } catch (Exception e) {
      logger.warn("conversion to java.net.URI failed for url: " + urlString);
      // This is falling back to existing approach, where we test for entire URL
      return HttpConnectionExecutionCapability.builder().url(urlString).scheme(null).port(null).hostName(null).build();
    }
  }

  public static HttpConnectionExecutionCapability buildHttpConnectionExecutionCapabilityForKms(String region) {
    String kmsUrl = generateKmsUrl(region);
    return buildHttpConnectionExecutionCapability(kmsUrl);
  }

  public static String generateKmsUrl(String region) {
    Regions regions = Regions.US_EAST_1;
    if (region != null) {
      regions = Regions.fromName(region);
    }
    // If it's an unknown region, will default to US_EAST_1's URL.
    return AWS_REGION_URL_MAP.containsKey(regions) ? AWS_REGION_URL_MAP.get(regions)
                                                   : AWS_REGION_URL_MAP.get(Regions.US_EAST_1);
  }

  private static String getHostName(URI uri) {
    if (isBlank(uri.getScheme()) && isBlank(uri.getHost())) {
      return uri.toString();
    }

    return uri.getHost();
  }
}
