// The number of approvals required to merge.
let numApprovalsRequired = 1;

const approvals = review.pullRequest.approvals;
const requestedTeams = _.pluck(review.pullRequest.requestedTeams, 'slug');


let numApprovals = _.where(approvals, 'approved').length;
const numRejections = _.where(approvals, 'changes_requested').length;

const discussionBlockers = _(review.discussions)
  .where({resolved: false})
  .pluck('participants')
  .flatten()
  .where({resolved: false})
  .map(user => _.pick(user, 'username'))
  .value();

let pendingReviewers = _(discussionBlockers)
  .map(user => _.pick(user, 'username'))
  .concat(review.pullRequest.requestedReviewers)
  .value();

const required = _.pluck(review.pullRequest.assignees, 'username');

_.pull(required, review.pullRequest.author.username);
if (required.length) {
  numApprovalsRequired =
    _.max([required.length, numApprovalsRequired]);
  numApprovals =
    (_(approvals).pick(required).where('approved').size()) +
    _.min([numApprovals, numApprovalsRequired - required.length]);
  pendingReviewers = _(required)
    .reject(username => approvals[username] === 'approved')
    .reject(
      username => pendingReviewers.length && approvals[username])
    .map(username => {username})
    .concat(pendingReviewers)
    .value();
}

pendingReviewers = _.uniq(pendingReviewers, 'username');

const completed = numApprovals >= numApprovalsRequired && Object.keys(requestedTeams).length == 0;

const description = (completed ? "✓" :
  (numRejections ? `${numRejections} change requests. ` : '') +
  (numApprovals < numApprovalsRequired ? `${numApprovals} of ${numApprovalsRequired} approvals obtained. ` : '') +
  (Object.keys(requestedTeams).length == 0 ? "" : `Waiting on team(s): ${requestedTeams}. `));

const shortDescription = (completed ? "✓" : "✗");

return {
  completed: completed, description, shortDescription, pendingReviewers
};
