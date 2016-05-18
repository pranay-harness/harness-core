package software.wings.utils;

import static org.apache.commons.csv.CSVFormat.DEFAULT;
import static software.wings.beans.ErrorConstants.INVALID_CSV_FILE;
import static software.wings.beans.ErrorConstants.UNKNOWN_ERROR_MEG;
import static software.wings.beans.Host.HostBuilder.aHost;

import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import software.wings.beans.Host;
import software.wings.exception.WingsException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 4/15/16.
 */
public class HostFileHelper {
  public static final String[] CSVHeader = {"HOST", "OS", "TAGS"};

  public static List<Host> parseHosts(InputStream inputStream, Host baseHost) {
    List<Host> hosts = new ArrayList<>();
    try {
      CSVParser csvParser = new CSVParser(new InputStreamReader(inputStream), DEFAULT.withHeader());
      List<CSVRecord> records = csvParser.getRecords();
      for (CSVRecord record : records) {
        String hostName = record.get("HOST");
        String osType = record.get("OS"); // TODO: Add tags ?
        hosts.add(aHost()
                      .withAppId(baseHost.getAppId())
                      .withInfraId(baseHost.getInfraId())
                      .withHostAttributes(baseHost.getHostAttributes())
                      .withBastionHostAttributes(baseHost.getBastionHostAttributes())
                      .withTags(baseHost.getTags())
                      .withHostName(hostName)
                      .build());
      }
    } catch (IOException ex) {
      throw new WingsException(INVALID_CSV_FILE);
    }
    return hosts;
  }

  public static File createHostsFile(List<Host> hosts) {
    File tempDir = FileUtils.createTempDirPath();
    File file = new File(tempDir, "Hosts.csv");
    FileWriter fileWriter = null;
    try {
      final CSVPrinter csvPrinter = new CSVPrinter(fileWriter, DEFAULT);
      fileWriter = new FileWriter(file);
      csvPrinter.printRecord(CSVHeader);
      hosts.forEach(host -> {
        List row = new ArrayList();
        row.add(host.getHostName()); // TODO: Add Tags ?
        try {
          csvPrinter.printRecord(row);
        } catch (IOException ex) {
          throw new WingsException(UNKNOWN_ERROR_MEG);
        }
      });
      fileWriter.flush();
    } catch (IOException ex) {
      throw new WingsException(UNKNOWN_ERROR_MEG);
    } finally {
      try {
        if (fileWriter != null) {
          fileWriter.close();
        }
      } catch (IOException ignore) {
      }
    }
    return file;
  }
}
