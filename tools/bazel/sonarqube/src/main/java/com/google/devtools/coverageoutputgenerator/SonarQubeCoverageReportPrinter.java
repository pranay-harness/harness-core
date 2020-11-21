package com.google.devtools.coverageoutputgenerator;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

class SonarQubeCoverageReportPrinter {
  private static final Logger log = Logger.getLogger(SonarQubeCoverageReportPrinter.class.getName());
  private final BufferedWriter bufferedWriter;

  private SonarQubeCoverageReportPrinter(BufferedWriter bufferedWriter) {
    this.bufferedWriter = bufferedWriter;
  }

  static boolean print(FileOutputStream outputStream, Coverage coverage) {
    BufferedWriter bufferedWriter;
    try (Writer fileWriter = new OutputStreamWriter(outputStream, UTF_8)) {
      bufferedWriter = new BufferedWriter(fileWriter);
      SonarQubeCoverageReportPrinter printer = new SonarQubeCoverageReportPrinter(bufferedWriter);
      printer.print(coverage);
      bufferedWriter.close();
    } catch (IOException exception) {
      log.log(Level.SEVERE, "Could not write to output file.");
      return false;
    }
    return true;
  }

  private boolean print(Coverage coverage) {
    try {
      bufferedWriter.write("<coverage version=\"1\">");
      bufferedWriter.newLine();

      for (SourceFileCoverage sourceFileCoverage : coverage.getAllSourceFiles()) {
        bufferedWriter.write("    <file path=\"" + sourceFileCoverage.sourceFileName() + "\">");
        bufferedWriter.newLine();

        for (LineCoverage line : sourceFileCoverage.getLines().values()) {
          List<BranchCoverage> branches = sourceFileCoverage.getAllBranches()
                                              .stream()
                                              .filter(b -> b.lineNumber() == line.lineNumber())
                                              .collect(Collectors.toList());

          bufferedWriter.write("        <lineToCover");
          bufferedWriter.write(" lineNumber=\"" + line.lineNumber() + "\"");
          bufferedWriter.write(" covered=\"" + (line.executionCount() > 0) + "\"");
          if (!branches.isEmpty()) {
            bufferedWriter.write(" branchesToCover=\"" + branches.size() + "\"");
            bufferedWriter.write(
                " coveredBranches=\"" + branches.stream().filter(BranchCoverage::wasExecuted).count() + "\"");
          }
          bufferedWriter.write("/>");
          bufferedWriter.newLine();
        }

        bufferedWriter.write("    </file>");
        bufferedWriter.newLine();
      }

      bufferedWriter.write("</coverage>");
    } catch (IOException exception) {
      log.log(Level.SEVERE, "Could not write to output file.");
      return false;
    }
    return true;
  }
}
