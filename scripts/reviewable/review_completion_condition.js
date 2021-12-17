// The number of approvals required to merge.
let numApprovalsRequired = 1;

let re = /(feat|fix|techdebt).*(:).*(\[CDP|\[PIE|\[PL)/g;

if (review.pullRequest.title.match(re)) {
  numApprovalsRequired = 2;
}

const approvals = review.pullRequest.approvals;
const requestedTeams = _.map(review.pullRequest.requestedTeams, "slug");

let numApprovals = _(approvals).filter(approval => approval === 'approved').value().length;
let numRejections = _(approvals).filter(approval => approval === 'changes_requested').value().length;

// Ignore system files (list of commits) for reviewing
_.forEach(review.systemFiles, (file) => {
  _.forEach(file.revisions, (rev) => {
    rev.reviewed = true;
  });
});

// Discussion can be resolved by anyone, find only blocking ones which don't have later satisfied comments
const discussionBlockers = _(review.discussions)
  .filter({ resolved: false })
  .reject(participant => isDiscussionResolved(participant))
  .reject(discussion => !discussion.target)
  .map(discussion => discussion.target.file + '#' + discussion.target.line)
  .value();

let pendingReviewers = [];
let required = _.map(review.pullRequest.assignees, "username");

_.pull(required, review.pullRequest.author.username);
if (required.length) {
  numApprovalsRequired = _.max([required.length, numApprovalsRequired]);
  numApprovals =
    _(approvals).pick(required).filter('approved').size() +
    _.min([numApprovals, numApprovalsRequired - required.length]);
  pendingReviewers = _(required)
    .reject(username => approvals[username] === 'approved')
    .reject(username => pendingReviewers.length && approvals[username])
    .map(username => ({username}))
    .concat(pendingReviewers)
    .value();
}

pendingReviewers = _.uniq(pendingReviewers, "username");

const olderCheck = Math.min(...Object.values(review.pullRequest.checks).map(check => check.timestamp))
const tooOld = olderCheck < Date.now() - 259200000;

// Find which files are not reviewed by required reviewers. Or by numApprovalsRequired of reviewers
let unreviewedFiles = [];
let fileBlockers = [];
_.forEach(review.files, function (file) {
  const lastRev = _(file.revisions).reject("obsolete").last();
  if (!lastRev) {
    // When there are reverted files it seems that all revisions on it are obsolete, so break early.
    return;
  }
  const reviewers = _(lastRev.reviewers)
    .map("username")
    .without(review.pullRequest.author.username)
    .value();
  const missingReviewers = _.difference(required, reviewers);
  if (reviewers.length >= numApprovalsRequired && _.isEmpty(missingReviewers))
    return;
  unreviewedFiles.add(file.path);
  const lastReviewedRev =
    _(file.revisions).findLast(rev => !_.isEmpty(rev.reviewers));
  fileBlockers = fileBlockers.concat(
    _.map(missingReviewers, username => ({username})),
    lastReviewedRev ? lastReviewedRev.reviewers : []
  );
});

const completed =
      !tooOld &&
      unreviewedFiles.length === 0 &&
      pendingReviewers.length === 0 &&
      numApprovals >= numApprovalsRequired &&
      discussionBlockers.length === 0 &&
      Object.keys(requestedTeams).length === 0;

const description = (completed ? "✓" :
  (tooOld ? `Some of the checks are too old. ` : '') +
  (unreviewedFiles.length > 0 ? `${unreviewedFiles.length} file(s) to review. ` + listFilesNeedingAttention() : '') +
  (numRejections ? `${numRejections} change requests. ` : '') +
  (discussionBlockers.length > 0 ? `${discussionBlockers.length}. unresolved discussions on files ` + listFilesNeedingAttention(discussionBlockers) + '': '') +
  (numApprovals < numApprovalsRequired ? `${numApprovals} of ${numApprovalsRequired} approvals obtained. ` : '') +
  (Object.keys(requestedTeams).length === 0 ? "" : `Waiting on team(s): ${requestedTeams}. `));

const shortDescription = completed ? "✓" : "✗ check last comment for details";

return {
  completed,
  description,
  shortDescription,
  pendingReviewers,
  files: review.files.concat(review.systemFiles)
};

// Discussion is resolved if there is a satisfied comment after a blocking comment
function isDiscussionResolved(discussion) {
  const participants = _(discussion.participants)
  .reject(participant => participant.username === review.pullRequest.author.username);

  const blocking = participants
  .reject((participant) => participant.resolved === true)
  .filter((participant) => participant.disposition === "blocking")
  .map((participant) => participant.lastActivityTimestamp)
  .max();

  const satisfied = participants
  .filter((participant) => participant.resolved === true)
  .filter((participant) => participant.disposition === "satisfied")
  .map((participant) => participant.lastActivityTimestamp)
  .max();

  return !blocking || satisfied > blocking;
}

function listFilesNeedingAttention(files) {
  if (files.length > 5) {
    return _(files).take(5).map(file => file.substring(file.lastIndexOf('/') + 1)).uniq().value().join(' ,') + '... ';
  } else {
    return _(files).map(file => file.substring(file.lastIndexOf('/') + 1)).uniq().value().join(' ,') + '. ';
  }
}