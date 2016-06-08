package software.wings.utils;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static software.wings.beans.ErrorConstants.INVALID_CSV_FILE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR;
import static software.wings.beans.Host.HostBuilder.aHost;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.exception.WingsException;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.TagService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 4/15/16.
 */
@Singleton
public class HostCsvFileHelper {
  private final Object[] CSVHeader = {
      "HOST_NAME", "HOST_CONNECTION_ATTRIBUTES", "BASTION_HOST_CONNECTION_ATTRIBUTES", "TAGS"};
  @Inject private SettingsService attributeService;
  @Inject private TagService tagService;
  private SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

  /**
   * Parses the hosts.
   *
   * @param infra       the infra
   * @param inputStream the input stream
   * @return the list
   */
  public List<Host> parseHosts(Infra infra, BoundedInputStream inputStream) {
    List<Host> hosts = new ArrayList<>();
    CSVParser csvParser = null;
    try {
      csvParser = new CSVParser(new InputStreamReader(inputStream, UTF_8), DEFAULT.withHeader());
      List<CSVRecord> records = csvParser.getRecords();
      for (CSVRecord record : records) {
        String hostName = record.get("HOST_NAME");
        SettingAttribute hostConnectionAttrs =
            attributeService.getByName(infra.getAppId(), record.get("HOST_CONNECTION_ATTRIBUTES"));
        SettingAttribute bastionHostAttrs =
            attributeService.getByName(infra.getAppId(), record.get("BASTION_HOST_CONNECTION_ATTRIBUTES"));
        String tagsString = record.get("TAGS");
        List<String> tagNames = tagsString != null && tagsString.length() > 0 ? asList(tagsString.split(",")) : null;
        List<Tag> tags = tagService.getTagsByName(infra.getAppId(), infra.getEnvId(), tagNames);

        hosts.add(aHost()
                      .withAppId(infra.getAppId())
                      .withInfraId(infra.getUuid())
                      .withHostName(hostName)
                      .withHostConnAttr(hostConnectionAttrs)
                      .withBastionConnAttr(bastionHostAttrs)
                      .withTags(tags)
                      .build());
      }
    } catch (IOException ex) {
      throw new WingsException(INVALID_CSV_FILE);
    } finally {
      Misc.quietClose(csvParser);
    }
    return hosts;
  }

  /**
   * Creates the hosts file.
   *
   * @param hosts the hosts
   * @return the file
   */
  public File createHostsFile(List<Host> hosts) {
    File tempDir = FileUtils.createTempDirPath();
    File file =
        new File(tempDir, String.format("Hosts_%s.csv", dateFormatter.format(new Date(System.currentTimeMillis()))));
    OutputStreamWriter fileWriter = null;
    try {
      fileWriter = new OutputStreamWriter(new FileOutputStream(file), UTF_8);
      final CSVPrinter csvPrinter = new CSVPrinter(fileWriter, DEFAULT);
      csvPrinter.printRecord(CSVHeader);
      hosts.forEach(host -> {
        List row = new ArrayList();
        row.add(host.getHostName());
        row.add(host.getHostConnAttr() != null ? host.getHostConnAttr().getName() : null);
        row.add(host.getBastionConnAttr() != null ? host.getBastionConnAttr().getName() : null);
        row.add(host.getTags() != null
                ? host.getTags().stream().map(tag -> tag.getName()).collect(Collectors.joining(","))
                : null);
        try {
          csvPrinter.printRecord(row);
        } catch (IOException e) {
          throw new WingsException(UNKNOWN_ERROR);
        } finally {
          Misc.quietClose(csvPrinter);
        }
      });
      fileWriter.flush();
      csvPrinter.close();
      fileWriter.close();
    } catch (Exception ex) {
      throw new WingsException(UNKNOWN_ERROR);
    }
    return file;
  }
}
