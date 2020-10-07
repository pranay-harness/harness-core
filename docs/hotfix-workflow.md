Hotfix workflow
===============

Branching model
---------------

We follow the below branching model:

- The branch `master` is the development trunk. Release branches are cut off this branch.
- The `release/saas/xxxxx` branches are cut from `master`, and deployed into QA.
- After QA signoff, the signed off builds for each component are deployed to production.

Once a release branch is cut, it is never merged back into `master`. Any change on the release branch needs to be manually cherry-picked onto `master`. Failing to do this may cause the fix to regress in a subsequent deployment.

QA hotfix
---------

- Create a branch off the branch deployed in QA.
- Fix the issue in one or more commits.
- In a separate commit, bump the build number in build.properties
- Raise a pr against the release branch deployed in QA.
- Get this pr merged, and deploy the resultant build to qa.

- Create a branch off master.
- Cherry pick the commit(s) with the fix in this branch.
- Raise a pr against master and get it merged.

Prod hotfix
-----------

- Create a branch off the release branch deployed in Prod.
- Fix the issue in one or more commits.
- In a separate commit, bump the build number in build.properties.
- Raise a pr against the release branch deployed in Prod.

- Create a branch off the release branch deployed in QA.
- Cherry pick the commit(s) with the fix in this branch.
- In a separate commit, bump the build number in build.properties
- Raise a pr against the release branch deployed in QA.

- Create a branch off master.
- Cherry pick the commit(s) with the fix in this branch.
- Raise a pr against master.
