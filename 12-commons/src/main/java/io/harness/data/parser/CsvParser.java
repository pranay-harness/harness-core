package io.harness.data.parser;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CsvParser {
  private static CSVFormat csvFormat = CSVFormat.DEFAULT.withTrim().withTrailingDelimiter().withEscape('\\');

  public static List<String> parse(String input) {
    List<String> list = new ArrayList<>();
    if (isEmpty(input)) {
      return list;
    }
    try (CSVParser csvParser = CSVParser.parse(input, csvFormat)) {
      final List<CSVRecord> records = csvParser.getRecords();
      if (records.size() == 0) {
        return list;
      }
      CSVRecord record = records.get(0);
      for (int i = 0; i < record.size(); i++) {
        list.add(record.get(i));
      }
    } catch (IOException e) {
      logger.error("", e);
    }
    return list;
  }
}
