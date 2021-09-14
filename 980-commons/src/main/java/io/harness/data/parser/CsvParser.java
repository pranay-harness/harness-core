/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.data.parser;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

@Slf4j
public class CsvParser {
  private CsvParser() {}
  private static CSVFormat csvFormat = CSVFormat.DEFAULT.withTrim().withTrailingDelimiter().withEscape('\\');

  public static List<String> parse(String input) {
    List<String> list = new ArrayList<>();
    if (isEmpty(input)) {
      return list;
    }
    try (CSVParser csvParser = CSVParser.parse(input, csvFormat)) {
      final List<CSVRecord> records = csvParser.getRecords();
      if (isEmpty(records)) {
        return list;
      }
      CSVRecord record = records.get(0);
      for (int i = 0; i < record.size(); i++) {
        list.add(record.get(i));
      }
    } catch (IOException e) {
      log.error("", e);
    }
    return list;
  }
}
