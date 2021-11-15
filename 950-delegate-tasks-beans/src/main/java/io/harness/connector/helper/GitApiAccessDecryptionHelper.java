package io.harness.connector.helper;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.DecryptableEntity;
import io.harness.delegate.beans.connector.scm.ScmConnector;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketApiAccessSpec;
import io.harness.delegate.beans.connector.scm.bitbucket.BitbucketConnector;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfig;
import io.harness.delegate.beans.connector.scm.github.GithubApiAccessSpec;
import io.harness.delegate.beans.connector.scm.github.GithubConnector;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabApiAccessSpec;
import io.harness.delegate.beans.connector.scm.gitlab.GitlabConnector;
import io.harness.exception.InvalidRequestException;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(DX)
public class GitApiAccessDecryptionHelper {
  public DecryptableEntity getAPIAccessDecryptableEntity(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnector) {
      return getAPIAccessDecryptableEntity((GithubConnector) scmConnector);
    } else if (scmConnector instanceof BitbucketConnector) {
      return getAPIAccessDecryptableEntity((BitbucketConnector) scmConnector);
    } else if (scmConnector instanceof GitlabConnector) {
      return getAPIAccessDecryptableEntity((GitlabConnector) scmConnector);
    }
    throw new InvalidRequestException("Unsupported Scm Connector");
  }

  public boolean hasApiAccess(ScmConnector scmConnector) {
    if (scmConnector instanceof GithubConnector) {
      return hasAPIAccess((GithubConnector) scmConnector);
    } else if (scmConnector instanceof BitbucketConnector) {
      return hasAPIAccess((BitbucketConnector) scmConnector);
    } else if (scmConnector instanceof GitlabConnector) {
      return hasAPIAccess((GitlabConnector) scmConnector);
    } else if (scmConnector instanceof GitConfig) {
      return false;
    }
    throw new InvalidRequestException("Unsupported Scm Connector");
  }

  private boolean hasAPIAccess(GithubConnector githubConnector) {
    return !(githubConnector == null || githubConnector.getApiAccess() == null
        || githubConnector.getApiAccess().getSpec() == null);
  }

  private boolean hasAPIAccess(BitbucketConnector bitbucketConnector) {
    return !(bitbucketConnector == null || bitbucketConnector.getApiAccess() == null
        || bitbucketConnector.getApiAccess().getSpec() == null);
  }

  private boolean hasAPIAccess(GitlabConnector gitlabConnector) {
    return !(gitlabConnector == null || gitlabConnector.getApiAccess() == null
        || gitlabConnector.getApiAccess().getSpec() == null);
  }

  public DecryptableEntity getAPIAccessDecryptableEntity(GithubConnector githubConnector) {
    if (githubConnector == null || githubConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return githubConnector.getApiAccess().getSpec();
  }

  public DecryptableEntity getAPIAccessDecryptableEntity(BitbucketConnector bitbucketConnector) {
    if (bitbucketConnector == null || bitbucketConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return bitbucketConnector.getApiAccess().getSpec();
  }

  public DecryptableEntity getAPIAccessDecryptableEntity(GitlabConnector gitlabConnector) {
    if (gitlabConnector == null || gitlabConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    return gitlabConnector.getApiAccess().getSpec();
  }

  public void setAPIAccessDecryptableEntity(ScmConnector scmConnector, DecryptableEntity decryptableEntity) {
    if (scmConnector instanceof GithubConnector) {
      setAPIAccessDecryptableEntity((GithubConnector) scmConnector, decryptableEntity);
    } else if (scmConnector instanceof BitbucketConnector) {
      setAPIAccessDecryptableEntity((BitbucketConnector) scmConnector, decryptableEntity);
    } else if (scmConnector instanceof GitlabConnector) {
      setAPIAccessDecryptableEntity((GitlabConnector) scmConnector, decryptableEntity);
    }
  }

  public void setAPIAccessDecryptableEntity(GithubConnector githubConnector, DecryptableEntity decryptableEntity) {
    if (githubConnector == null || githubConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    githubConnector.getApiAccess().setSpec((GithubApiAccessSpec) decryptableEntity);
  }

  public void setAPIAccessDecryptableEntity(
      BitbucketConnector bitbucketConnector, DecryptableEntity decryptableEntity) {
    if (bitbucketConnector == null || bitbucketConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    bitbucketConnector.getApiAccess().setSpec((BitbucketApiAccessSpec) decryptableEntity);
  }

  public void setAPIAccessDecryptableEntity(GitlabConnector gitlabConnector, DecryptableEntity decryptableEntity) {
    if (gitlabConnector == null || gitlabConnector.getApiAccess() == null) {
      throw new InvalidRequestException("The given connector doesn't have api access field set");
    }
    gitlabConnector.getApiAccess().setSpec((GitlabApiAccessSpec) decryptableEntity);
  }
}
