package io.harness.checks;

import io.harness.checks.buildpulse.client.BuildPulseClient;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.xml.sax.SAXException;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import static java.util.Arrays.asList;

@Slf4j
public class Main {
  private static BuildPulseClient getBuildPulseClient(String baseUrl, String authToken) {
    Duration halfMinute = Duration.ofSeconds(30);
    OkHttpClient okHttpClient =
        new OkHttpClient()
            .newBuilder()
            .connectTimeout(halfMinute)
            .readTimeout(halfMinute)
            .writeTimeout(halfMinute)
            .addInterceptor(chain
                -> chain.proceed(chain.request().newBuilder().header("Authorization", "token " + authToken).build()))
            .build();
    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .client(okHttpClient)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
    return retrofit.create(BuildPulseClient.class);
  }

  public static void main(String[] args)
      throws IOException, ParserConfigurationException, SAXException, XPathExpressionException, TransformerException {
    String phase = args[0];
    if (phase.equals("check")) {
      check(args);
    } else {
      suppressFlakes(args);
    }
  }

  private static void suppressFlakes(String[] args)
      throws IOException, ParserConfigurationException, SAXException, TransformerException, XPathExpressionException {
    String url = Objects.requireNonNull(System.getenv("BUILDPULSE_URL"), "BUILDPULSE_URL missing");
    String token = Objects.requireNonNull(System.getenv("BUILDPULSE_TOKEN"), "BUILDPULSE_TOKEN missing");
    String baseDir = args[1];
    double maxFailChance = Double.parseDouble(args[2]);
    BuildPulseClient buildPulseClient = getBuildPulseClient(url, token);
    Set<String> flakyTests = new FlakeFinder(buildPulseClient, maxFailChance).fetchFlakyTests();
    logger.info("Found {} flaky tests with maxFailChance = {}", flakyTests.size(), maxFailChance);
    logger.info("Flaky tests are: \n{}", String.join("\n", flakyTests));
    List<String> surefireReports = new ReportFinder(baseDir).findSurefireReports();
    ReportProcessor reportProcessor = new ReportProcessor(flakyTests);
    boolean success = true;
    for (String report : surefireReports) {
      logger.info("Processing report file {}", report);
      success = reportProcessor.removeFlakyTestsAndCheckSuccess(report) && success;
    }
    if (!success) {
      logger.warn("Found non-flaky test failures");
    }
  }

  private static void check(String[] args)
      throws IOException, XPathExpressionException, SAXException, ParserConfigurationException {
    String baseDir = args[1];
    int maxFailures = Integer.parseInt(args[2]);
    List<String> surefireReports = new ReportFinder(baseDir).findSurefireReports();
    ReportProcessor reportProcessor = new ReportProcessor(null);
    int numFailures = 0;
    for (String report : surefireReports) {
      int failureCount = reportProcessor.getFailureCount(report);

      // keep this report significantly different the issue one to be searchable
      if (failureCount == 0) {
        logger.info("{} - is clean", report);
      } else {
        logger.info("{} - has {} issue(s)", report, failureCount);
      }
      numFailures += failureCount;
    }
    logger.info("Total number of failed tests: {}", numFailures);
    if (numFailures >= maxFailures) {
      logger.error("Too many test failures");
      System.exit(1);
    }
  }
}
