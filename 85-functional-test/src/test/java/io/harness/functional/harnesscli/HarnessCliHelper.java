package io.harness.functional.harnesscli;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
@Slf4j
public class HarnessCliHelper {
  public List<String> executeCLICommand(String command) throws IOException {
    String s;
    Process processFinal = Runtime.getRuntime().exec(command);
    List<String> cliOutput = new ArrayList<>();
    InputStream inputStream = null;
    inputStream = processFinal.getInputStream();
    BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));
    while ((s = processStdErr.readLine()) != null) {
      cliOutput.add(s);
    }
    return cliOutput;
  }
  public List<String> getCLICommandError(String command) throws IOException {
    String s;
    Process processFinal = Runtime.getRuntime().exec(command);
    List<String> cliOutput = new ArrayList<>();
    InputStream inputStream = null;
    inputStream = processFinal.getErrorStream();
    BufferedReader processStdErr = new BufferedReader(new InputStreamReader(inputStream));
    while ((s = processStdErr.readLine()) != null) {
      cliOutput.add(s);
    }
    return cliOutput;
  }
}
