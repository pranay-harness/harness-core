package software.wings.utils;

import static io.harness.eraro.ErrorCode.INVALID_CSV_FILE;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static software.wings.beans.infrastructure.Host.Builder.aHost;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import software.wings.beans.SettingAttribute;
import software.wings.beans.infrastructure.Host;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.SettingsService;

import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/15/16.
 */
@Singleton
public class HostCsvFileHelper {
  private final Object[] CSVHeader = {
      "HOST_NAME", "HOST_CONNECTION_ATTRIBUTES", "BASTION_HOST_CONNECTION_ATTRIBUTES", "TAGS"};
  @Inject private SettingsService attributeService;
  @Inject private AppService appService;
  @SuppressFBWarnings("URF_UNREAD_FIELD")
  private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  public List<Host> parseHosts(String infraId, String appId, String envId, BoundedInputStream inputStream) {
    String accountId = appService.getAccountIdByAppId(appId);
    List<Host> hosts = new ArrayList<>();
    try (CSVParser csvParser = new CSVParser(new InputStreamReader(inputStream, UTF_8), DEFAULT.withHeader())) {
      List<CSVRecord> records = csvParser.getRecords();
      for (CSVRecord record : records) {
        String hostName = record.get("HOST_NAME");
        SettingAttribute hostConnectionAttrs =
            attributeService.getByName(accountId, appId, record.get("HOST_CONNECTION_ATTRIBUTES"));
        SettingAttribute bastionHostAttrs =
            attributeService.getByName(accountId, appId, record.get("BASTION_HOST_CONNECTION_ATTRIBUTES"));
        String tagsString = record.get("TAGS");
        List<String> tagNames = tagsString != null && tagsString.length() > 0 ? asList(tagsString.split(",")) : null;

        hosts.add(aHost()
                      .withAppId(appId)
                      .withHostName(hostName)
                      .withHostConnAttr(hostConnectionAttrs.getUuid())
                      .withBastionConnAttr(bastionHostAttrs.getUuid())
                      .build());
      }
    } catch (IOException ex) {
      throw new WingsException(INVALID_CSV_FILE);
    }
    return hosts;
  }
}
