package io.harness.delegate.beans.executioncapability;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class TcpConnectivityExecutionCapabilityTest {
  private static final String HOST = "host";
  private static final int PORT = 22;
  private static final String PORT_STR = String.valueOf(PORT);

  @Test
  @Category(UnitTests.class)
  public void testGetCapabilityType() {
    ExecutionCapability ec = new TcpConnectivityExecutionCapability(HOST, PORT);
    assertEquals(CapabilityType.NETCAT, ec.getCapabilityType());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchCapabilityBasis() {
    ExecutionCapability ec = new TcpConnectivityExecutionCapability(HOST, PORT);
    assertEquals(String.join(":", HOST, PORT_STR), ec.fetchCapabilityBasis());
  }

  @Test
  @Category(UnitTests.class)
  public void testProcessExecutorArguments() {
    TcpConnectivityExecutionCapability ec = new TcpConnectivityExecutionCapability(HOST, PORT);
    List<String> args = ec.processExecutorArguments();
    assertEquals(5, args.size());
    assertEquals(HOST, args.get(3));
    assertEquals(PORT_STR, args.get(4));
  }
}