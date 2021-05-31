// cg package contains functions to interact with the cg in db
package mongodb

import (
	"context"
	"fmt"
	"github.com/kamva/mgm/v3"
	"github.com/mattn/go-zglob"
	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/db"
	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/addon/ti"
	"github.com/wings-software/portal/product/ci/ti-service/cg"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.uber.org/zap"
)

type CgServiceImpl struct {
	MongoDb db.Persistence
	Log     *zap.SugaredLogger
}

const (
	nodeColl  = "nodes"
	relnsColl = "relations"
)

// NewRelation creates Relation object form given fields
func NewCgServiceImpl(db db.MongoDb, log *zap.SugaredLogger) *CgServiceImpl {
	return &CgServiceImpl{
		MongoDb: &db,
		Log:     log,
	}
}

type Relation struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Source  int        `json:"source" bson:"source"`
	Tests   []int      `json:"tests" bson:"tests"`
	Acct    string     `json:"account" bson:"account"`
	Proj    string     `json:"project" bson:"project"`
	Org     string     `json:"organization" bson:"organization"`
	VCSInfo cg.VCSInfo `json:"vcs_info" bson:"vcs_info"`
}

type Node struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Package string     `json:"package" bson:"package"`
	Method  string     `json:"method" bson:"method"`
	Id      int        `json:"id" bson:"id"`
	Params  string     `json:"params" bson:"params"`
	Class   string     `json:"class" bson:"class"`
	Type    string     `json:"type" bson:"type"`
	Acct    string     `json:"account" bson:"account"`
	Proj    string     `json:"project" bson:"project"`
	Org     string     `json:"organization" bson:"organization"`
	VCSInfo cg.VCSInfo `json:"vcs_info" bson:"vcs_info"`
}

// NewNode creates Node object form given fields
func NewNode(id int, pkg, method, params, class, typ string, vcs cg.VCSInfo, acc, org, proj string) *Node {
	return &Node{
		Id:      id,
		Package: pkg,
		Method:  method,
		Params:  params,
		Class:   class,
		Type:    typ,
		Acct:    acc,
		Org:     org,
		Proj:    proj,
		VCSInfo: vcs,
	}
}

// NewRelation creates Relation object form given fields
func NewRelation(source int, tests []int, vcs cg.VCSInfo, acc, org, proj string) *Relation {
	return &Relation{
		Source:  source,
		Tests:   tests,
		Acct:    acc,
		Org:     org,
		Proj:    proj,
		VCSInfo: vcs,
	}
}

// queryHelper gets the tests that need to be run corresponding to the packages and classes
func (cgs *CgServiceImpl) queryHelper(targetBranch, repo string, pkgs, classes []string) ([]types.RunnableTest, error) {
	if len(pkgs) != len(classes) {
		return nil, fmt.Errorf("length of pkgs: %d and length of classes: %d don't match", len(pkgs), len(classes))
	}
	if len(pkgs) == 0 {
		cgs.Log.Warnw("did not receive any pkg/classes to query DB")
		return []types.RunnableTest{}, nil
	}
	result := []types.RunnableTest{}
	// Query 1
	// Get nodes corresponding to the packages and classes
	nodes := []Node{}
	allowedPairs := []interface{}{}
	for idx, pkg := range pkgs {
		cls := classes[idx]
		allowedPairs = append(allowedPairs,
			bson.M{"package": pkg, "class": cls,
				"vcs_info.repo":   repo,
				"vcs_info.branch": targetBranch})
	}
	err := mgm.Coll(&Node{}).SimpleFind(&nodes, bson.M{"$or": allowedPairs})
	if err != nil {
		return nil, err
	}
	if len(nodes) == 0 {
		// Log error message for debugging if no nodes are found
		cgs.Log.Errorw("could not find any nodes corresponding to the pkgs and classes",
			"pkgs", pkgs, "classes", classes, "repo", repo, "branch", targetBranch)
	}

	nids := []int{}
	for _, n := range nodes {
		nids = append(nids, n.Id)
	}

	// Query 2
	// Get unique test IDs corresponding to these nodes
	relations := []Relation{}
	err = mgm.Coll(&Relation{}).SimpleFind(&relations,
		bson.M{"source": bson.M{"$in": nids},
			"vcs_info.branch": targetBranch,
			"vcs_info.repo":   repo})
	if err != nil {
		return nil, err
	}
	mtids := make(map[int]struct{})
	tids := []int{}
	for _, t := range relations {
		for _, tid := range t.Tests {
			if _, ok := mtids[tid]; !ok {
				tids = append(tids, tid)
				mtids[tid] = struct{}{}
			}
		}
	}

	// Query 3
	// Get test information corresponding to test IDs
	tnodes := []Node{}
	err = mgm.Coll(&Node{}).SimpleFind(&tnodes,
		bson.M{"id": bson.M{"$in": tids},
			"type":            "test",
			"vcs_info.branch": targetBranch,
			"vcs_info.repo":   repo})
	if err != nil {
		return nil, err
	}
	if len(tnodes) != len(tids) {
		// Log error message for debugging if we don't find a test ID in the node list
		cgs.Log.Errorw("number of elements in test IDs and retrieved nodes don't match",
			"test IDs", tids, "nodes", tnodes, "length(test ids)", len(tids), "length(nodes)", len(tnodes))
	}
	for _, t := range tnodes {
		result = append(result, types.RunnableTest{Pkg: t.Package, Class: t.Class})
	}

	return result, nil
}

// isValid checks whether the test is valid or not
func isValid(t types.RunnableTest) bool {
	return t.Pkg != "" && t.Class != ""
}

func (cgs *CgServiceImpl) GetTestsToRun(ctx context.Context, req types.SelectTestsReq) (types.SelectTestsResp, error) {
	cgs.Log.Infow("getTestsToRun call", "req", req)
	// parse package and class names from the files
	fileNames := []string{}
	for _, f := range req.Files {
		// Check if the filename matches any of the regexes in the ignore config. If so, remove them
		// from consideration
		var remove bool
		for _, ignore := range req.TiConfig.Config.Ignore {
			matched, _ := zglob.Match(ignore, f.Name)
			if matched == true {
				// TODO: (Vistaar) Remove this warning message in prod since it has no context
				// Keeping for debugging help for now
				cgs.Log.Warnw(fmt.Sprintf("removing %s from consideration as it matches %s", f, ignore))
				remove = true
				break
			}
		}
		if !remove {
			fileNames = append(fileNames, f.Name)
		}
	}
	deletedTests := make(map[types.RunnableTest]struct{})
	// Add deleted tests to a map to remove them from the final list
	for _, f := range req.Files {
		if f.Status != types.FileDeleted {
			continue
		}
		n, err := utils.ParseFileNames([]string{f.Name})
		if err != nil {
			// Ignore errors
			continue
		}
		deletedTests[types.RunnableTest{Pkg: n[0].Pkg, Class: n[0].Class}] = struct{}{}
	}
	res := types.SelectTestsResp{}
	totalTests := 0
	nodes, err := utils.ParseFileNames(fileNames)
	if err != nil {
		return res, err
	}

	// Get list of all tests with unique pkg/class information
	all := []Node{}
	err = mgm.Coll(&Node{}).SimpleFind(&all, bson.M{"type": "test", "vcs_info.branch": req.TargetBranch, "vcs_info.repo": req.Repo})
	if err != nil {
		return res, err
	}
	// Test methods corresponding to each <package, class>
	methodMap := make(map[types.RunnableTest][]types.RunnableTest)
	for _, t := range all {
		u := types.RunnableTest{Pkg: t.Package, Class: t.Class}
		methodMap[u] = append(methodMap[u], types.RunnableTest{Pkg: t.Package, Class: t.Class, Method: t.Method})
		totalTests += 1
	}

	m := make(map[types.RunnableTest]struct{}) // Get unique tests to run
	l := []types.RunnableTest{}
	var pkgs []string
	var cls []string
	var selectAll bool
	updated := 0
	new := 0 // Keep track of new files. Don't count them in the current total. The count will get updated using partial CG
	for _, node := range nodes {
		// A file which is not recognized. Need to add logic for handling these type of files
		if !utils.IsSupported(node) {
			// A list with a single empty element indicates that all tests need to be run
			selectAll = true
		} else if utils.IsTest(node) {
			t := types.RunnableTest{Pkg: node.Pkg, Class: node.Class}
			if !isValid(t) {
				cgs.Log.Errorw("received test without pkg/class as input")
			} else {
				// If there is any test which was deleted in this PR, don't process it
				if _, ok := deletedTests[t]; ok {
					cgs.Log.Warnw(fmt.Sprintf("removing test %s from selection as it was deleted", t))
					continue
				}
				// Test is valid, add the test
				if _, ok := m[t]; !ok { // hasn't been added before
					// Figure out the type of the test. If it exists in cnt,
					// then it is updated otherwise new
					if _, ok2 := methodMap[t]; !ok2 {
						t.Selection = types.SelectNewTest
						// Mark Methods field as * since it's a new test.
						// We can get method information only from the PCG.
						new++
						t.Method = "*"
						l = append(l, t)
					} else {
						updated += len(methodMap[t])
						for _, upd := range methodMap[t] {
							l = append(l, types.RunnableTest{Pkg: upd.Pkg, Class: upd.Class, Method: upd.Method, Selection: types.SelectUpdatedTest})
						}
					}
					m[t] = struct{}{}
				}
			}
		} else {
			// Source file
			pkgs = append(pkgs, node.Pkg)
			cls = append(cls, node.Class)
		}
	}
	if selectAll == true {
		return types.SelectTestsResp{
			SelectAll:     true,
			TotalTests:    totalTests,
			SelectedTests: totalTests,
			UpdatedTests:  updated,
			SrcCodeTests:  totalTests - updated,
		}, nil
	}

	tests, err := cgs.queryHelper(req.TargetBranch, req.Repo, pkgs, cls)
	if err != nil {
		return res, err
	}
	for _, t := range tests {
		if !isValid(t) {
			cgs.Log.Errorw("found test without pkg/class data in mongo")
		} else {
			// If there is any test which was deleted in this PR, don't process it
			if _, ok := deletedTests[t]; ok {
				cgs.Log.Warnw(fmt.Sprintf("removing test %s from selection as it was deleted", t))
				continue
			}
			// Test is valid, add the test
			if _, ok := m[t]; !ok { // hasn't been added before
				m[t] = struct{}{}
				for _, src := range methodMap[t] {
					l = append(l, types.RunnableTest{Pkg: src.Pkg, Class: src.Class,
						Method: src.Method, Selection: types.SelectSourceCode})
				}
			}
		}
	}
	return types.SelectTestsResp{
		TotalTests:    totalTests,
		SelectedTests: len(l) - new, // new tests will be added later in upsert with uploading of partial CG
		UpdatedTests:  updated,
		SrcCodeTests:  len(l) - updated - new,
		Tests:         l,
	}, nil
}

// UploadPartialCg uploads callgraph corresponding to a branch in PR run in mongo.
func (cgs *CgServiceImpl) UploadPartialCg(ctx context.Context, cg *ti.Callgraph, info cg.VCSInfo, acc, org, proj, target string) (types.SelectTestsResp, error) {
	nodes := make([]Node, len(cg.Nodes))
	rels := make([]Relation, len(cg.Relations))

	resp := types.SelectTestsResp{}

	// Create method map to calculate how many tests have been added
	all := []Node{}
	err := mgm.Coll(&Node{}).SimpleFind(&all, bson.M{"type": "test", "vcs_info.branch": target, "vcs_info.repo": info.Repo})
	if err != nil {
		return resp, err
	}
	methodMap := make(map[types.RunnableTest]bool)
	for _, t := range all {
		u := types.RunnableTest{Pkg: t.Package, Class: t.Class, Method: t.Method}
		methodMap[u] = true
	}

	for i, node := range cg.Nodes {
		nodes[i] = *NewNode(node.ID, node.Package, node.Method, node.Params, node.Class, node.Type, info, acc, org, proj)
		if node.Type != "test" {
			continue
		}
		// If the node is a test node, check whether it exists in the existing callgraph or not
		if _, ok := methodMap[types.RunnableTest{Pkg: node.Package, Class: node.Class, Method: node.Method}]; !ok {
			resp.NewTests += 1
			resp.TotalTests += 1
		}
	}
	for i, rel := range cg.Relations {
		rels[i] = *NewRelation(rel.Source, rel.Tests, info, acc, org, proj)
	}
	// query for partial callgraph for the filter -(repo + branch + (commitId != currentCommit)) and delete old entries.
	// this will delete all the nodes create by older commits for current pull request
	f := bson.M{"vcs_info.repo": info.Repo, "vcs_info.branch": info.Branch, "vcs_info.commit_id": bson.M{"$ne": info.CommitId}}
	r1, err := cgs.MongoDb.DeleteMany(nodeColl, ctx, f, &options.DeleteOptions{})
	if err != nil {
		return resp, errors.Wrap(
			err,
			fmt.Sprintf("failed to delete old records from nodes collection while uploading partial callgraph for"+
				" repo: %s, branch: %s, acc: %s", info.Repo, info.Branch, acc))
	}
	// this will delete all the relations create by older commits for current pull request
	f = bson.M{"vcs_info.repo": info.Repo, "vcs_info.branch": info.Branch, "vcs_info.commit_id": bson.M{"$ne": info.CommitId}}
	r2, err := cgs.MongoDb.DeleteMany(relnsColl, ctx, f, &options.DeleteOptions{})
	if err != nil {
		return resp, errors.Wrap(
			err,
			fmt.Sprintf("failed to delete records from relations collection while uploading partial callgraph "+
				"for repo: %s, branch: %s acc: %s", info.Repo, info.Branch, acc))
	}
	cgs.Log.Infow(
		fmt.Sprintf("deleted %d records from nodes and %d records from relns collection",
			r1.DeletedCount, r2.DeletedCount), "accountId", acc, "repo", info.Repo, "branch", info.Branch)

	err = cgs.upsertNodes(ctx, nodes, info)
	if err != nil {
		return resp, err
	}
	err = cgs.upsertRelations(ctx, rels, info)
	if err != nil {
		return resp, err
	}
	return resp, nil
}

// todo(Aman): Figure out a way to automatically update updatedBy and updatedAt fields. Manually updating it is not scalable.
// todo aman -- ["vcs_info.repo" : repo] this filter needs to be added to all the queries. Currently it will not work as we
// don't have repo and branch populated in master callgraph uploaded in db
// MergePartialCg merges partial callgraph of from source branch to dest branch in case corresponding pr is merged
// It also cleans up the nodes which have been deleted in the PR from nodes and relations collections.
func (cgs *CgServiceImpl) MergePartialCg(ctx context.Context, req types.MergePartialCgRequest) error {
	commit := req.Diff.Sha
	branch := req.TargetBranch
	repo := utils.GetRepoUrl(req.Repo)
	files := req.Diff.Files

	// merging nodes
	// get all the nids which are from the dest branch
	f := bson.M{"vcs_info.branch": branch, "vcs_info.repo": repo}
	dNids, err := cgs.getNodeIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	// get all the nids from the source branch which need to be merged
	f = bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo}
	srcNids, err := cgs.getNodeIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}

	// list of new nodes in src branch
	nodesToMove := utils.GetSliceDiff(srcNids, dNids)
	err = cgs.mergeNodes(ctx, commit, branch, repo, nodesToMove)
	if err != nil {
		return err
	}

	// merge relations
	// get all the nids which are from the dest branch
	f = bson.M{"vcs_info.branch": branch, "vcs_info.repo": repo}
	dRelIDs, err := cgs.getRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	// get all the nids from the source branch which need to be merged
	f = bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo}
	sRelIDs, err := cgs.getRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	err = cgs.mergeRelations(ctx, commit, branch, repo, sRelIDs, dRelIDs)
	if err != nil {
		return err
	}

	// handle deletion of files and corresponding entries from nodes and relations table.
	deletedF := []string{}
	for _, f := range files {
		if f.Status == types.FileDeleted {
			deletedF = append(deletedF, f.Name)
		}
	}

	// if deleted files are empty, there are no nodes and relations to update
	if len(deletedF) == 0 {
		return nil
	}

	n, err := utils.ParseFileNames(deletedF)
	cgs.Log.Infow(fmt.Sprintf("deleted %d files", len(n)),
		"repo", repo,
		"branch", branch,
		"commit", commit,
	)

	// condition for fetching ids of nodes which are deleted
	cond := []interface{}{}
	for _, v := range n {
		cond = append(cond, bson.M{"package": v.Pkg, "class": v.Class, "vcs_info.branch": branch, "vcs_info.repo": repo})
	}
	f = bson.M{"$or": cond}
	var tNodes []Node
	err = cgs.MongoDb.Find(nodeColl, ctx, f, &tNodes)
	if err != nil {
		return formatError(err, "failed to query nodes coll for deleted files", repo, branch, commit)
	}
	delIDs := []int{} // delIDs is list of ids of nodes which are deleted
	for _, node := range tNodes {
		delIDs = append(delIDs, node.Id)
	}
	cgs.Log.Infow(fmt.Sprintf("node ids to be deleted: [%v]", delIDs), "branch", branch, "repo", repo)
	if len(delIDs) > 0 {
		// delete nodes with id in delIDs
		f = bson.M{"id": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch}
		r, err := cgs.MongoDb.DeleteMany(nodeColl, ctx, f, &options.DeleteOptions{})
		if err != nil {
			return formatError(err, fmt.Sprintf("failed to delete records from nodes coll delIDs: %v", delIDs), repo, branch, commit)
		}

		// delete relations with source in delIDs
		f = bson.M{"source": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch}
		r1, err := cgs.MongoDb.DeleteMany(relnsColl, ctx, f, &options.DeleteOptions{})
		if err != nil {
			return formatError(err, fmt.Sprintf("failed to delete records from relns coll delIDs: %v", delIDs), repo, branch, commit)
		}
		cgs.Log.Infow(fmt.Sprintf("deleted %d, %d records from nodes, relations collection for deleted files",
			r.DeletedCount, r1.DeletedCount), "branch", branch, "repo", repo)

		// update tests fields which contains delIDs in relations
		f = bson.M{"tests": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch}
		update := bson.M{"$pull": bson.M{"tests": bson.M{"$in": delIDs}}}
		res, err := cgs.MongoDb.UpdateMany(relnsColl, ctx, f, update)
		if err != nil {
			return formatError(err, "failed to get records in relations collection", repo, branch, commit)
		}
		cgs.Log.Infow(fmt.Sprintf("matched %d, updated %d records from relations collection for deleted files",
			res.MatchedCount, res.ModifiedCount), "branch", branch, "repo", repo)
	}
	return nil
}

func formatError(err error, msg, repo, branch, commit string) error {
	return errors.Wrap(
		err,
		fmt.Sprintf("%s, repo: %s, in branch: %s, commit: %v",
			msg, repo, branch, commit))
}

// merge merges two slices and returns the union of them
func merge(tests []int, tests2 []int) []int {
	relMap := make(map[int]bool)
	for _, test := range tests {
		relMap[test] = true
	}
	for _, test := range tests2 {
		relMap[test] = true
	}
	keys := make([]int, len(relMap))
	i := 0
	for k := range relMap {
		keys[i] = k
		i++
	}
	return keys
}

// getNodeIds queries mongo and returns list of node ID's for the given filter
func (cgs *CgServiceImpl) getNodeIds(ctx context.Context, commit, branch, repo string, f bson.M) ([]int, error) {
	var nodes []Node
	var nIds []int
	err := cgs.MongoDb.Find(nodeColl, ctx, f, &nodes)
	if err != nil {
		return []int{}, formatError(err, "failed in find query in nodes collection", repo, branch, commit)
	}
	for _, v := range nodes {
		nIds = append(nIds, v.Id)
	}
	return nIds, nil
}

// getRelIds queries mongo and returns list of relation ID's for the given filter
func (cgs *CgServiceImpl) getRelIds(ctx context.Context, commit, branch, repo string, f bson.M) ([]int, error) {
	var relations []Relation
	var relIDS []int
	err := cgs.MongoDb.Find(relnsColl, ctx, f, &relations)
	if err != nil {
		return []int{}, formatError(err, "failed in find query in rel collection", repo, branch, commit)
	}
	for _, v := range relations {
		relIDS = append(relIDS, v.Source)
	}
	return relIDS, nil
}

// mergeNodes merges records in nodes collection in case of a pr merge from source branch to destination branch
// #1: Move unique nodes records in src branch to destination branch
// #2; delete all entries in src branch as the merging is complete.
func (cgs *CgServiceImpl) mergeNodes(ctx context.Context, commit, branch, repo string, nodesToMove []int) error {
	// update `branch` field of the nodes from source to dest
	if len(nodesToMove) > 0 {
		f := bson.M{"vcs_info.commit_id": commit, "id": bson.M{"$in": nodesToMove}, "vcs_info.repo": repo}
		update := bson.M{"$set": bson.M{"vcs_info.branch": branch}}
		res, err := cgs.MongoDb.UpdateMany(nodeColl, ctx, f, update)
		if err != nil {
			return formatError(err, "failed to merge cg in nodes collection for", repo, branch, commit)
		}
		cgs.Log.Infow(
			fmt.Sprintf("matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// delete remaining records of src branch from nodes collection
	// todo(AMAN):  find a better filter than $ne
	f := bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo, "vcs_info.branch": bson.M{"$ne": branch}}
	res, err := cgs.MongoDb.DeleteMany(nodeColl, ctx, f, &options.DeleteOptions{})
	if err != nil {
		return formatError(err, "failed to delete records in nodes collection", repo, branch, commit)
	}
	cgs.Log.Infow(fmt.Sprintf("deleted %d records from nodes collection", res.DeletedCount),
		"repo", repo,
		"branch", branch,
		"commit", commit,
	)
	return nil
}

// mergeRelations merges records in relation collection in case of a pr merge from source branch to destination branch
// #1: Move unique relation records in src branch to destination branch
// #2: for source which exists in both  src and destination branch, merge tests form both and update tests of dest branch
// #3; delete all entries in src branch as the merging is complete.
func (cgs *CgServiceImpl) mergeRelations(ctx context.Context, commit, branch, repo string, sIDs []int, dIDs []int) error {
	// list of new relToMove in src branch
	relToMove := utils.GetSliceDiff(sIDs, dIDs)
	// moving relations records
	// update `branch` field of the relToMove from source to dest
	if len(relToMove) > 0 {
		f := bson.M{"vcs_info.commit_id": commit, "source": bson.M{"$in": relToMove}, "vcs_info.repo": repo}
		u := bson.M{"$set": bson.M{"vcs_info.branch": branch}}
		res, err := cgs.MongoDb.UpdateMany(relnsColl, ctx, f, u)
		if err != nil {
			return formatError(err, "failed to merge cg in nodes collection for", repo, branch, commit)
		}
		cgs.Log.Infow(
			fmt.Sprintf("moving records: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// updating commons records in relations collection in source and destination branches
	var srcRelation, destRelation []Relation
	relIDToUpdate := utils.GetSliceDiff(sIDs, relToMove)
	cgs.Log.Infow("updating relations",
		"relIDToUpdate", relIDToUpdate,
		"len(sIDs)", len(sIDs),
		"len(relToMove)", len(relToMove),
		"repo", repo,
		"branch", branch,
		"commit", commit)
	if len(relIDToUpdate) > 0 {
		f := bson.M{"vcs_info.branch": branch, "source": bson.M{"$in": relIDToUpdate}, "vcs_info.repo": repo}
		// filter for getting relations from destination branch
		err := cgs.MongoDb.Find(relnsColl, ctx, f, &destRelation)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", repo, branch, commit)
		}
		// filter for getting relations from source branch
		f = bson.M{"vcs_info.commit_id": commit, "source": bson.M{"$in": relIDToUpdate}, "vcs_info.repo": repo}
		err = cgs.MongoDb.Find(relnsColl, ctx, f, &srcRelation)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", repo, branch, commit)
		}
		destMap := getRelMap(srcRelation, destRelation)
		var operations []mongo.WriteModel
		for src, tests := range destMap {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"source": src, "vcs_info.repo": repo, "vcs_info.branch": branch})
			operation.SetUpdate(bson.M{"$set": bson.M{"tests": tests}})
			operations = append(operations, operation)
		}
		res, err := cgs.MongoDb.BulkWrite(relnsColl, ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to merge relations collection", repo, branch, commit)
		}
		cgs.Log.Infow(
			fmt.Sprintf("relations merge: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// delete remaining records of src branch from relations collection
	// todo(AMAN):  find a better filter than $ne
	f := bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo, "vcs_info.branch": bson.M{"$ne": branch}}
	res, err := cgs.MongoDb.DeleteMany(relnsColl, ctx, f, &options.DeleteOptions{})
	if err != nil {
		return formatError(err, "failed to delete records in relations collection", repo, branch, commit)
	}
	cgs.Log.Infow(fmt.Sprintf("deleted %d records from relation collection", res.DeletedCount),
		"repo", repo,
		"branch", branch,
		"commit", commit,
	)
	return nil
}

// upsertNodes upserts partial callgraph for a repo + branch + commitId. Steps are:
// New nodes received in the cg will be added to db only if they are not already present in db.
// The algo to do it is
// 1. get list of node ids for `repo` + `branch` + `commmit_id`. These nodes are uploaded as part of some other job running
// for the same PR.
// 2. In new nodes received as part of current pr callgraph, only the nodes which are not already present in db will be created.
// it is checked using Id key. If the Id already exists, skip the node.
func (cgs *CgServiceImpl) upsertNodes(ctx context.Context, nodes []Node, info cg.VCSInfo) error {
	cgs.Log.Infow("uploading partialcg in nodes collection",
		"#nodes", len(nodes), "repo", info.Repo, "branch", info.Branch)
	// fetch existing records for branch
	f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.commit_id": info.CommitId, "vcs_info.repo": info.Repo}
	NIds, err := cgs.getNodeIds(ctx, info.CommitId, info.Branch, info.Repo, f)
	if err != nil {
		return err
	}
	existingNodes := getMap(NIds)
	nodesToAdd := make([]interface{}, 0)
	for _, node := range nodes {
		if !existingNodes[node.Id] {
			nodesToAdd = append(nodesToAdd, node)
		}
	}
	if len(nodesToAdd) > 0 {
		res, err := cgs.MongoDb.InsertMany(nodeColl, ctx, nodesToAdd)
		if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("failed to add nodes while uploading partial cg, repo: %s, branch: %s", info.Repo, info.Branch))
		}
		cgs.Log.Infow(fmt.Sprintf("inserted %d records in nodes collection", len(res.InsertedIDs)),
			"repo", info.Repo,
			"branch", info.Branch,
		)
	}
	return nil
}

// upsertRelations is used to upload partial callagraph to db. If there is already a cg present with
// the same commit, it udpdates that callgraph otherwise creates a new entry The algo for that is:
// 1. get all the existing relations for `repo` + `branch` + `commit_id`.
// 2. Relations received in cg which are new will be inserted in relations collection.
// relations which are already present in the db needs to be merged.
func (cgs *CgServiceImpl) upsertRelations(ctx context.Context, relns []Relation, info cg.VCSInfo) error {
	cgs.Log.Infow("uploading partialcg in relations collection",
		"#relns", len(relns), "repo", info.Repo, "branch", info.Branch)
	// fetch existing records for branch
	f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.commit_id": info.CommitId, "vcs_info.repo": info.Repo}
	Ids, err := cgs.getRelIds(ctx, info.CommitId, info.Branch, info.Repo, f)
	if err != nil {
		return err
	}
	existingRel := getMap(Ids)
	relToAdd := make([]interface{}, 0)
	relToUpdate := make([]Relation, 0)
	for _, rel := range relns {
		if existingRel[rel.Source] {
			relToUpdate = append(relToUpdate, rel)
		} else {
			relToAdd = append(relToAdd, rel)
		}
	}
	if len(relToAdd) > 0 {
		res, err := cgs.MongoDb.InsertMany(relnsColl, ctx, relToAdd)
		if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("failed to add relns while uploading partial cg, repo: %s, branch: %s", info.Repo, info.Branch))
		}
		cgs.Log.Infow(fmt.Sprintf("inserted %d records in relns collection", len(res.InsertedIDs)),
			"repo", info.Repo,
			"branch", info.Branch,
		)
	}
	if len(relToUpdate) > 0 {
		var idToUpdate []int
		var existingRelns []Relation
		for _, rel := range relToUpdate {
			idToUpdate = append(idToUpdate, rel.Source)
		}
		f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.repo": info.Repo, "source": bson.M{"$in": idToUpdate}}
		err := cgs.MongoDb.Find(relnsColl, ctx, f, &existingRelns)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", info.Repo, info.Repo, info.CommitId)
		}
		finalRelations := getRelMap(relToUpdate, existingRelns)
		var operations []mongo.WriteModel
		for src, tests := range finalRelations {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"source": src, "vcs_info.repo": info.Repo, "vcs_info.branch": info.Branch})
			operation.SetUpdate(bson.M{"$set": bson.M{"tests": tests}})
			operations = append(operations, operation)
		}
		res, err := cgs.MongoDb.BulkWrite(relnsColl, ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to update relations collection", info.Repo, info.Branch, info.CommitId)
		}
		cgs.Log.Infow(
			fmt.Sprintf("relations updated: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"repo", info.Repo,
			"branch", info.Branch,
			"commit", info.CommitId,
		)
	}
	return nil
}

// getMap takes slice of int as an input and returns a map with all elements of slice as keys
func getMap(ids []int) map[int]bool {
	mp := make(map[int]bool)
	for _, id := range ids {
		mp[id] = true
	}
	return mp
}

// getRelMap takes two Relation records A and B and returns a map[source]tests object
// where tests is the union of tests of A and B for each entry of A
func getRelMap(src []Relation, dest []Relation) map[int][]int {
	srcMap := make(map[int][]int)
	destMap := make(map[int][]int)
	for _, relation := range src {
		srcMap[relation.Source] = relation.Tests
	}
	for _, v := range dest {
		destMap[v.Source] = v.Tests
	}
	for k, v := range destMap {
		destMap[k] = merge(v, srcMap[k])
	}
	return destMap
}
