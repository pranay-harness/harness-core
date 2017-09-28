package software.wings.yaml.gitSync;

import org.eclipse.jgit.api.TransportConfigCallback;
import org.eclipse.jgit.transport.SshSessionFactory;
import org.eclipse.jgit.transport.SshTransport;
import org.eclipse.jgit.transport.Transport;

public class CustomTransportConfigCallback implements TransportConfigCallback {
  private SshSessionFactory sshSessionFactory;

  public CustomTransportConfigCallback(SshSessionFactory sshSessionFactory) {
    this.sshSessionFactory = sshSessionFactory;
  }

  @Override
  public void configure(Transport transport) {
    SshTransport sshTransport = (SshTransport) transport;
    sshTransport.setSshSessionFactory(sshSessionFactory);
  }
}
