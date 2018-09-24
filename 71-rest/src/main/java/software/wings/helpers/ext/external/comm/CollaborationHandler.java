package software.wings.helpers.ext.external.comm;

public interface CollaborationHandler {
  CollaborationProviderResponse handle(CollaborationProviderRequest request);

  boolean validateDelegateConnection(CollaborationProviderRequest request);
}
