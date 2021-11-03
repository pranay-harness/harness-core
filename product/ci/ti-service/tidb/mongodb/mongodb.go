package mongodb

import (
	"container/list"
	"context"
	"fmt"
	"strings"
	"time"

	"github.com/kamva/mgm/v3"
	"github.com/mattn/go-zglob"
	"github.com/pkg/errors"
	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"

	"github.com/wings-software/portal/commons/go/lib/utils"
	"github.com/wings-software/portal/product/ci/ti-service/logger"
	"github.com/wings-software/portal/product/ci/ti-service/types"
	"go.mongodb.org/mongo-driver/bson"
	"go.mongodb.org/mongo-driver/mongo"
	"go.mongodb.org/mongo-driver/mongo/options"
	"go.mongodb.org/mongo-driver/mongo/readpref"

	"go.uber.org/zap"
)

type MongoDb struct {
	Client   *mongo.Client
	Database *mongo.Database
}

func getDefaultConfig() *mgm.Config {
	// TODO: (vistaar) Decrease this to a reasonable value (1 second or so).
	// Right now queries are slow while running locally.
	return &mgm.Config{
		CtxTimeout: 15 * time.Second,
	}
}

type Relation struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Source   int       `json:"source" bson:"source"`
	Tests    []int     `json:"tests" bson:"tests"`
	Acct     string    `json:"account" bson:"account"`
	Proj     string    `json:"project" bson:"project"`
	Org      string    `json:"organization" bson:"organization"`
	ExpireAt time.Time `json:"expireAt" bson:"expireAt,omitempty"` // only include field if it's not set to a zero value
	VCSInfo  VCSInfo   `json:"vcs_info" bson:"vcs_info"`
}

type Node struct {
	// DefaultModel adds _id,created_at and updated_at fields to the Model
	mgm.DefaultModel `bson:",inline"`

	Package         string    `json:"package" bson:"package"`
	Method          string    `json:"method" bson:"method"`
	Id              int       `json:"id" bson:"id"`
	ClassId         int       `json:"classId" bson:"classId"`
	Params          string    `json:"params" bson:"params"`
	File            string    `json:"file" bson:"file"` // Only populated if the type is resource
	Class           string    `json:"class" bson:"class"`
	Type            string    `json:"type" bson:"type"` // Can be test | source | resource
	CallsReflection bool      `json:"callsReflection" bson:"callsReflection"`
	Acct            string    `json:"account" bson:"account"`
	Proj            string    `json:"project" bson:"project"`
	Org             string    `json:"organization" bson:"organization"`
	ExpireAt        time.Time `json:"expireAt" bson:"expireAt,omitempty"` // only include field if it's not set to a zero value
	VCSInfo         VCSInfo   `json:"vcs_info" bson:"vcs_info"`
}

type VisEdge struct {
	mgm.DefaultModel `bson:",inline"`

	Caller   int       `json:"caller" bson:"caller"`
	Callee   []int     `json:"callee" bson:"callee"`
	Acct     string    `json:"account" bson:"account"`
	Proj     string    `json:"project" bson:"project"`
	Org      string    `json:"organization" bson:"organization"`
	VCSInfo  VCSInfo   `json:"vcs_info" bson:"vcs_info"`
	ExpireAt time.Time `json:"expireAt" bson:"expireAt,omitempty"` // only include field if it's not set to a zero value
}

const (
	nodeColl  = "nodes"
	relnsColl = "relations"
	visColl   = "vis_edges"
)

var expireQuery = bson.M{"$set": bson.M{"expireAt": time.Now()}}

// VCSInfo contains metadata corresponding to version control system details
type VCSInfo struct {
	Repo     string `json:"repo" bson:"repo"`
	Branch   string `json:"branch" bson:"branch"`
	CommitId string `json:"commit_id" bson:"commit_id"`
}

// NewNode creates Node object form given fields
func NewNode(id, classId int, pkg, method, params, class, typ, file string, callsReflection bool, vcs VCSInfo, acc, org, proj string) *Node {
	return &Node{
		DefaultModel: mgm.DefaultModel{
			DateFields: mgm.DateFields{
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			},
		},
		Id:              id,
		ClassId:         classId,
		Package:         pkg,
		Method:          method,
		Params:          params,
		Class:           class,
		Type:            typ,
		File:            file,
		CallsReflection: callsReflection,
		Acct:            acc,
		Org:             org,
		Proj:            proj,
		VCSInfo:         vcs,
	}
}

// NewRelation creates Relation object form given fields
func NewRelation(source int, tests []int, vcs VCSInfo, acc, org, proj string) *Relation {
	return &Relation{
		DefaultModel: mgm.DefaultModel{
			DateFields: mgm.DateFields{
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			},
		},
		Source:  source,
		Tests:   tests,
		Acct:    acc,
		Org:     org,
		Proj:    proj,
		VCSInfo: vcs,
	}
}

func NewVisEdge(caller int, callee []int, account, org, project string, vcs VCSInfo) *VisEdge {
	return &VisEdge{
		DefaultModel: mgm.DefaultModel{
			DateFields: mgm.DateFields{
				CreatedAt: time.Now(),
				UpdatedAt: time.Now(),
			},
		},
		Caller:  caller,
		Callee:  callee,
		Acct:    account,
		Org:     org,
		Proj:    project,
		VCSInfo: vcs,
	}
}

func New(username, password, host, port, dbName string, connStr string, log *zap.SugaredLogger) (*MongoDb, error) {
	// If any connStr is provided, use that
	if connStr == "" {
		connStr = fmt.Sprintf("mongodb://%s:%s/?connect=direct", host, port)
	}

	log.Infow("trying to connect to mongo", "connStr", connStr)
	ctx := context.Background()
	opts := options.Client().ApplyURI(connStr)
	if len(username) > 0 {
		credential := options.Credential{
			Username: username,
			Password: password,
		}
		opts = opts.SetAuth(credential)
	}
	err := mgm.SetDefaultConfig(getDefaultConfig(), dbName, opts)
	if err != nil {
		return nil, err
	}
	_, client, _, err := mgm.DefaultConfigs()
	if err != nil {
		return nil, err
	}

	// Ping mongo server to see if it's accessible. This is a requirement for startup
	// of TI service.
	err = client.Ping(ctx, readpref.Primary())
	if err != nil {
		return nil, err
	}

	log.Infow("successfully pinged mongo server")
	return &MongoDb{Client: client, Database: client.Database(dbName)}, nil
}

// queryHelper gets the tests that need to be run corresponding to the parsed file nodes
// We return true if all the tests need to be selected
func (mdb *MongoDb) queryHelper(ctx context.Context, targetBranch, repo string, fn []utils.Node, account string) ([]types.RunnableTest, bool, error) {
	result := []types.RunnableTest{}
	// Query 1
	// Get node IDs corresponding to the packages, classes and resources by iterating over the parsed file nodes
	nodes := []Node{}
	mResources := make(map[string]struct{})
	allowedPairs := []interface{}{}
	for _, n := range fn {
		// It's possible test files might also have source methods. We should get tests
		// which are dependent on those as well.
		if n.Type == utils.NodeType_SOURCE || n.Type == utils.NodeType_TEST {
			allowedPairs = append(allowedPairs,
				bson.M{"type": "source", "package": n.Pkg, "class": n.Class,
					"vcs_info.repo":   repo,
					"vcs_info.branch": targetBranch,
					"account":         account})
		} else if n.Type == utils.NodeType_RESOURCE {
			// There can be multiple resource files with the same name
			if _, ok := mResources[n.File]; ok {
				continue
			}
			mResources[n.File] = struct{}{}
			allowedPairs = append(allowedPairs,
				bson.M{"type": "resource", "file": n.File,
					"vcs_info.repo":   repo,
					"vcs_info.branch": targetBranch,
					"account":         account})
		}
	}
	if len(allowedPairs) == 0 {
		// Nothing to query in DB
		return result, false, nil
	}
	err := mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &nodes, bson.M{"$or": allowedPairs})
	if err != nil {
		return nil, false, err
	}

	nids := []int{}
	nMap := make(map[int]struct{})
	rCount := 0
	// Get unique node IDs corresponding to the conditions.
	// If the number of resources from the query is less than the number of resources changed,
	// some of the resources have not been mapped.
	for _, n := range nodes {
		if _, ok := nMap[n.Id]; ok {
			continue
		}
		nMap[n.Id] = struct{}{}
		nids = append(nids, n.Id)
		// Check whether all the resource nodes are present in the mapping or not.
		if n.Type == "resource" {
			rCount++
		}
	}
	if rCount < len(mResources) { // Run all the tests
		return result, true, nil
	}

	// Query 2
	// Get unique test IDs corresponding to these nodes
	relations := []Relation{}
	err = mgm.Coll(&Relation{}).SimpleFindWithCtx(ctx, &relations,
		bson.M{"source": bson.M{"$in": nids},
			"vcs_info.branch": targetBranch,
			"vcs_info.repo":   repo,
			"account":         account})
	if err != nil {
		return nil, false, err
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
	err = mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &tnodes,
		bson.M{"id": bson.M{"$in": tids},
			"type":            "test",
			"vcs_info.branch": targetBranch,
			"vcs_info.repo":   repo,
			"account":         account})
	if err != nil {
		return nil, false, err
	}
	for _, t := range tnodes {
		result = append(result, types.RunnableTest{Pkg: t.Package, Class: t.Class})
	}

	return result, false, nil
}

func toVis(n Node, imp bool) types.VisNode {
	return types.VisNode{Id: n.ClassId, Package: n.Package, Class: n.Class, File: n.File, Type: n.Type, Important: imp}
}

/* Helper function which returns corresponding visualisation nodes that led to a test being run.
pkg and cls here are the package and the class of the test which is run and files are the list
of changed files in the PR. */
func getVisDirectRelations(ctx context.Context, files []types.File, branch, repo, pkg, cls, account string) ([]types.VisNode, error) {
	fl := []string{}
	resp := []types.VisNode{}
	for _, f := range files {
		fl = append(fl, f.Name)
	}
	fn, _ := utils.ParseFileNames(fl) // Get file nodes corresponding to changed list

	nodes := []Node{}
	mResources := make(map[string]struct{})
	allowedPairs := []interface{}{}
	for _, n := range fn {
		if n.Type == utils.NodeType_SOURCE {
			allowedPairs = append(allowedPairs,
				bson.M{"type": "source", "package": n.Pkg, "class": n.Class,
					"vcs_info.repo":   repo,
					"vcs_info.branch": branch,
					"account":         account})
		} else if n.Type == utils.NodeType_RESOURCE {
			// There can be multiple resource files with the same name
			if _, ok := mResources[n.File]; ok {
				continue
			}
			mResources[n.File] = struct{}{}
			allowedPairs = append(allowedPairs,
				bson.M{"type": "resource", "file": n.File,
					"vcs_info.repo":   repo,
					"vcs_info.branch": branch,
					"account":         account})
		}
	}
	if len(allowedPairs) == 0 {
		return resp, nil
	}

	// Check the node IDs corresponding to this query
	all := []Node{}
	err := mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &all, bson.M{"$or": allowedPairs})
	if err != nil {
		return resp, err
	}
	if len(all) == 0 { // No nodes were found for any file changes
		return resp, nil
	}
	nids := []int{}
	// Get node IDs
	for _, n := range all {
		nids = append(nids, n.Id)
	}

	// Get test IDs
	q := bson.M{"vcs_info.repo": repo,
		"vcs_info.branch": branch,
		"account":         account, "package": pkg, "class": cls}
	nodes = []Node{}
	err = mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &nodes, q)
	if err != nil {
		return resp, err
	}
	tids := []int{}
	for _, n := range nodes {
		tids = append(tids, n.Id)
	}

	// Check if the test nodes are present in any of these source id mappings
	q = bson.M{"account": account, "vcs_info.repo": repo, "vcs_info.branch": branch, "source": bson.M{"$in": nids}, "tests": bson.M{"$in": tids}}
	relns := []Relation{}
	err = mgm.Coll(&Relation{}).SimpleFindWithCtx(ctx, &relns, q)
	if err != nil {
		return resp, err
	}
	if len(relns) == 0 {
		return resp, nil
	}

	validNodes := make(map[int]struct{})
	for _, r := range relns {
		validNodes[r.Source] = struct{}{}
	}

	for _, n := range all {
		if _, ok := validNodes[n.Id]; ok {
			resp = append(resp, toVis(n, true))
		}
	}
	return resp, nil
}

// isValid checks whether the test is valid or not
func isValid(t types.RunnableTest) bool {
	return t.Pkg != "" && t.Class != ""
}

/*
	Callgraph:
		1 -> 2, 3, 6, 8
		2 -> 4, 7
		3 -> 2, 8

	BFS(1) with a limit x and source as 1 should return BFS with root node as 1 and maximum of x nodes.

*/
func (mdb *MongoDb) GetVg(ctx context.Context, req types.GetVgReq) (types.GetVgResp, error) {
	ret := types.GetVgResp{}
	var pkg, cls string
	var branch string

	branch = req.SourceBranch // Try to search for the visualisation data in the source branch

	if req.Class != "" {
		// This will be of the form <pkg.class>. Parse out the package and class names from it.
		idx := strings.LastIndex(req.Class, ".")
		if idx == -1 {
			return ret, fmt.Errorf("incorrectly formatted class name: %s", req.Class)
		}
		pkg = req.Class[:idx]
		cls = req.Class[idx+1:]

		// Try to see if we have any information present in the source branch
		fi := bson.M{"vcs_info.branch": branch, "vcs_info.repo": req.Repo, "account": req.AccountId, "package": pkg, "class": cls}
		err := mdb.Database.Collection(nodeColl).FindOne(ctx, fi, &options.FindOneOptions{}).Decode(&bson.M{})
		if err != nil {
			if err == mongo.ErrNoDocuments {
				// If nothing is present in the source branch, we try to get information from the target branch
				logger.FromContext(ctx).Infow("could not find a source branch visualization mapping. Defaulting to target branch",
					"source_branch", req.SourceBranch, "target_branch", req.TargetBranch, "account", req.AccountId, "repo",
					req.Repo, "package", pkg, "class", cls)
				branch = req.TargetBranch
			} else {
				return ret, err
			}
		}

		resp, err := mdb.bfsWithSource(ctx, branch, pkg, cls, req)
		if err != nil {
			return resp, err
		}

		// Get the direct relations among the changed files which led to this test being run
		f, err := getVisDirectRelations(ctx, req.DiffFiles, branch, req.Repo, pkg, cls, req.AccountId)
		if err != nil {
			logger.FromContext(ctx).Errorw("could not get linked files for visualization mapping", "diff_files", req.DiffFiles, "branch", branch,
				"repo", req.Repo, "package", pkg, "class", cls, "account", req.AccountId)
		}
		for _, k := range f {
			resp.Nodes = append(resp.Nodes, k)
		}
		return formatVis(resp), nil
	} else {
		// Get a partial BFS corresponding to any random nodes
		branch = req.TargetBranch
		resp, err := mdb.bfsRandom(ctx, branch, req)
		if err != nil {
			return resp, err
		}
		return formatVis(resp), nil
	}
}

// formatVis removes duplicate class ID nodes from the visualisation response
func formatVis(inp types.GetVgResp) types.GetVgResp {
	out := types.GetVgResp{Edges: inp.Edges}
	m := make(map[int]struct{})
	for _, vn := range inp.Nodes {
		if _, ok := m[vn.Id]; ok {
			continue
		}
		m[vn.Id] = struct{}{}
		out.Nodes = append(out.Nodes, vn)
	}
	return out
}

// Perform a BFS starting for given branch and repo with the source nodes as <pkg, class>
func (mdb *MongoDb) bfsWithSource(ctx context.Context, branch, pkg, cls string, req types.GetVgReq) (types.GetVgResp, error) {
	// Try to query the package and class in the target branch
	ret := types.GetVgResp{}
	all := []Node{}
	fi := bson.M{"vcs_info.branch": branch, "vcs_info.repo": req.Repo, "account": req.AccountId, "package": pkg, "class": cls}
	err := mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &all, fi)
	if err != nil {
		return ret, err
	}
	// The node was not found in the branch
	if len(all) == 0 {
		return ret, fmt.Errorf("could not find an entry corresponding to: %s.%s", pkg, cls)
	}

	// Perform the BFS
	src := []int{}
	// Initialize the starting nodes
	for _, s := range all {
		src = append(src, s.ClassId)
	}

	return mdb.bfsHelper(ctx, src, []int{}, branch, req)

}

// Perform a random BFS over the search space to return a partial graph
func (mdb *MongoDb) bfsRandom(ctx context.Context, branch string, req types.GetVgReq) (types.GetVgResp, error) {
	all := []Node{}
	ret := types.GetVgResp{}
	opt := options.FindOptions{Limit: &req.Limit}
	fi := bson.M{"vcs_info.branch": branch, "vcs_info.repo": req.Repo, "account": req.AccountId}
	err := mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &all, fi, &opt)
	if err != nil {
		return ret, err
	}
	if len(all) == 0 {
		return ret, errors.New("no data present in visualisation callgraph")
	}
	src := []int{}
	add := []int{}
	for idx, s := range all {
		if idx == 0 {
			src = append(src, s.ClassId)
		} else {
			add = append(add, s.ClassId)
		}
	}

	return mdb.bfsHelper(ctx, src, add, branch, req)
}

// bfsHelper takes in a list of source nodes to start the BFS from and a list of additional nodes which can be used to
// add in more nodes to the BFS if required. It returns all the nodes and edges for this graph.
func (mdb *MongoDb) bfsHelper(ctx context.Context, srcList, addList []int, branch string, req types.GetVgReq) (types.GetVgResp, error) {
	Q := list.New()
	vis := make(map[int]struct{})
	ret := types.GetVgResp{}
	idx := 0

	// Add all source nodes to the queue
	for _, n := range srcList {
		Q.PushBack(n)
	}

	nIds := []int{}
	for len(nIds) < int(req.Limit) && Q.Len() > 0 {
		id := Q.Front()
		val := id.Value.(int)
		Q.Remove(id)

		if _, ok := vis[val]; !ok {
			nIds = append(nIds, val) // Add to node IDs if not added before
			m := types.VisMapping{}
			m.From = val
			vis[val] = struct{}{} // Mark the node as visited
			// Go through edges of id and add the nodes to the queue
			edge := VisEdge{}
			fi := bson.M{"vcs_info.branch": branch, "vcs_info.repo": req.Repo, "account": req.AccountId, "caller": val}
			err := mdb.Database.Collection(visColl).FindOne(ctx, fi, &options.FindOneOptions{}).Decode(&edge)
			if err != nil {
				if err != mongo.ErrNoDocuments {
					// Node has no edges
					return ret, err
				}
			} else {
				// Construct the response.
				for _, k := range edge.Callee {
					Q.PushBack(k)
					m.To = append(m.To, k)
				}
				ret.Edges = append(ret.Edges, m)
			}
		}

		// Start a BFS from a different random node
		if Q.Len() == 0 && idx <= len(addList)-1 {
			Q.PushBack(addList[idx])
			idx++
		}
	}

	// Get detailed node information
	all := []Node{}
	f := bson.M{"vcs_info.branch": branch, "vcs_info.repo": req.Repo, "account": req.AccountId, "classId": bson.M{"$in": nIds}}
	err := mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &all, f)
	if err != nil {
		return ret, err
	}

	for _, n := range all {
		v := toVis(n, false)
		if isImportant(v, req.DiffFiles) {
			v.Important = true
		}
		// If the id is in the source from where we performed the BFS, mark it as a root node
		if contains(srcList, n.ClassId) {
			v.Root = true
		}
		ret.Nodes = append(ret.Nodes, v)
	}

	return ret, nil
}

func contains(s []int, check int) bool {
	for _, k := range s {
		if k == check {
			return true
		}
	}
	return false
}

// Check whether the vis node occurs in the changed files list or not
func isImportant(vn types.VisNode, diffFiles []types.File) bool {
	for _, f := range diffFiles {
		n, _ := utils.ParseJavaNode(f.Name)
		if vn.Type == "resource" { // Resource type
			if vn.File == n.File {
				return true
			}
		} else { // Source or test type
			if vn.Package == n.Pkg && vn.Class == n.Class {
				return true
			}
		}
	}
	return false
}

func (mdb *MongoDb) GetTestsToRun(ctx context.Context, req types.SelectTestsReq, account string, enableReflection bool) (types.SelectTestsResp, error) {
	// parse package and class names from the files
	fileNames := []string{}
	for _, f := range req.Files {
		// Check if the filename matches any of the regexes in the ignore config. If so, remove them
		// from consideration
		var remove bool
		for _, ignore := range req.TiConfig.Config.Ignore {
			matched, _ := zglob.Match(ignore, f.Name)
			if matched == true {
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
	fnodes, err := utils.ParseFileNames(fileNames)
	if err != nil {
		return res, err
	}

	// Get list of all tests with unique pkg/class information
	all := []Node{}
	err = mgm.Coll(&Node{}).SimpleFindWithCtx(ctx, &all, bson.M{"type": "test", "vcs_info.branch": req.TargetBranch, "vcs_info.repo": req.Repo, "account": account})
	if err != nil {
		return res, err
	}
	// Test methods corresponding to each <package, class>
	methodMap := make(map[types.RunnableTest][]types.RunnableTest)
	idMap := make(map[int]struct{})
	reflectionTests := []types.RunnableTest{}
	for _, t := range all {
		if _, ok := idMap[t.Id]; ok { // Only add unique IDs in the map
			continue
		}
		u := types.RunnableTest{Pkg: t.Package, Class: t.Class}
		methodMap[u] = append(methodMap[u], types.RunnableTest{Pkg: t.Package, Class: t.Class, Method: t.Method})
		if t.CallsReflection {
			reflectionTests = append(reflectionTests, u)
		}
		totalTests += 1
		idMap[t.Id] = struct{}{}
	}

	// If no tests were found in the target branch, we want to run all the tests to generate the callgraph for that branch
	if req.SelectAll == true || totalTests == 0 {
		return types.SelectTestsResp{
			SelectAll:    true,
			TotalTests:   totalTests,
			SrcCodeTests: totalTests,
		}, nil
	}

	m := make(map[types.RunnableTest]struct{}) // Get unique tests to run
	l := []types.RunnableTest{}
	var selectAll bool
	updated := 0
	new := 0 // Keep track of new files. Don't count them in the current total. The count will get updated using partial CG
	for _, node := range fnodes {
		// A file which is not recognized. Need to add logic for handling these type of files
		if !utils.IsSupported(node) {
			// A list with a single empty element indicates that all tests need to be run
			selectAll = true
		} else if utils.IsTest(node) {
			t := types.RunnableTest{Pkg: node.Pkg, Class: node.Class}
			if !isValid(t) {
				logger.FromContext(ctx).Errorw("received test without pkg/class as input")
			} else {
				// If there is any test which was deleted in this PR, don't process it
				if _, ok := deletedTests[t]; ok {
					logger.FromContext(ctx).Warnw(fmt.Sprintf("removing test %s from selection as it was deleted", t))
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

	// Get tests corresponding to parsed source and resource file nodes
	tests, runAll, err := mdb.queryHelper(ctx, req.TargetBranch, req.Repo, fnodes, account)
	if err != nil {
		return res, err
	}
	if runAll {
		return types.SelectTestsResp{
			SelectAll:     true,
			TotalTests:    totalTests,
			SelectedTests: totalTests,
			UpdatedTests:  updated,
			SrcCodeTests:  totalTests - updated,
		}, nil
	}
	for _, t := range tests {
		if !isValid(t) {
			logger.FromContext(ctx).Errorw("found test without pkg/class data in mongo")
		} else {
			// If there is any test which was deleted in this PR, don't process it
			if _, ok := deletedTests[t]; ok {
				logger.FromContext(ctx).Warnw(fmt.Sprintf("removing test %s from selection as it was deleted", t))
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

	if enableReflection {
		// Go through reflection tests and add anything that hasn't been added before
		for _, rt := range reflectionTests {
			if _, ok := m[rt]; !ok {
				m[rt] = struct{}{}
				for _, src := range methodMap[rt] {
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
func (mdb *MongoDb) UploadPartialCg(ctx context.Context, cg *cgp.Callgraph, info VCSInfo, account, org, proj, target string) (types.SelectTestsResp, error) {
	resp := types.SelectTestsResp{}
	if len(cg.Nodes) == 0 && len(cg.TestRelations) == 0 {
		// Don't delete the existing callgraph, this might happen in case of some issues with the setup
		return resp, nil
	}
	nodes := make([]Node, len(cg.Nodes))
	rels := make([]Relation, len(cg.TestRelations))
	visEdges := make([]VisEdge, len(cg.VisRelations))

	// Create method map to calculate how many tests have been added
	all := []Node{}
	err := mgm.Coll(&Node{}).SimpleFind(&all, bson.M{"type": "test", "vcs_info.branch": target, "vcs_info.repo": info.Repo, "account": account})
	if err != nil {
		return resp, err
	}
	methodMap := make(map[types.RunnableTest]bool)
	for _, t := range all {
		u := types.RunnableTest{Pkg: t.Package, Class: t.Class, Method: t.Method}
		methodMap[u] = true
	}

	for i, node := range cg.Nodes {
		nodes[i] = *NewNode(node.ID, node.ClassId, node.Package, node.Method, node.Params, node.Class, node.Type, node.File, node.CallsReflection, info, account, org, proj)
		if node.Type != "test" {
			continue
		}
		// If the node is a test node, check whether it exists in the existing callgraph or not
		if _, ok := methodMap[types.RunnableTest{Pkg: node.Package, Class: node.Class, Method: node.Method}]; !ok {
			resp.NewTests += 1
			resp.TotalTests += 1
		}
	}
	for i, rel := range cg.TestRelations {
		rels[i] = *NewRelation(rel.Source, rel.Tests, info, account, org, proj)
	}
	for i, vis := range cg.VisRelations {
		visEdges[i] = *NewVisEdge(vis.Source, vis.Tests, account, org, proj, info)
	}
	// query for partial callgraph for the filter -(repo + branch + (commitId != currentCommit)) and delete old entries.
	// this will delete all the nodes create by older commits for current pull request
	f := bson.M{"vcs_info.repo": info.Repo, "account": account, "vcs_info.branch": info.Branch, "vcs_info.commit_id": bson.M{"$ne": info.CommitId}}
	r1, err := mdb.Database.Collection(nodeColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return resp, errors.Wrap(
			err,
			fmt.Sprintf("failed to delete old records from nodes collection while uploading partial callgraph for"+
				" repo: %s, branch: %s, acc: %s", info.Repo, info.Branch, account))
	}
	// this will delete all the relations create by older commits for current pull request
	f = bson.M{"vcs_info.repo": info.Repo, "account": account, "vcs_info.branch": info.Branch, "vcs_info.commit_id": bson.M{"$ne": info.CommitId}}
	r2, err := mdb.Database.Collection(relnsColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return resp, errors.Wrap(
			err,
			fmt.Sprintf("failed to make records from relations collection expired while uploading partial callgraph "+
				"for repo: %s, branch: %s acc: %s", info.Repo, info.Branch, account))
	}
	// this will delete all the vis_relations created by older commits for current pull request
	f = bson.M{"vcs_info.repo": info.Repo, "account": account, "vcs_info.branch": info.Branch, "vcs_info.commit_id": bson.M{"$ne": info.CommitId}}
	r3, err := mdb.Database.Collection(visColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return resp, errors.Wrap(
			err,
			fmt.Sprintf("failed to expire records from vis_edges collection "+
				"repo: %s, branch: %s acc: %s", info.Repo, info.Branch, account))
	}
	logger.FromContext(ctx).Infow(
		fmt.Sprintf("expired records in nodes: %d, relations:  %d, vis_edges: %d collection",
			r1.ModifiedCount, r2.ModifiedCount, r3.ModifiedCount), "account", account, "repo", info.Repo, "branch", info.Branch)

	err = mdb.upsertNodes(ctx, nodes, info, account)
	if err != nil {
		return resp, err
	}
	err = mdb.upsertTestRelations(ctx, rels, info, account)
	if err != nil {
		return resp, errors.Wrap(err, fmt.Sprintf("failed to write in %s", relnsColl))
	}
	err = mdb.upsertVisRelations(ctx, visEdges, info, account)
	if err != nil {
		return resp, errors.Wrap(err, fmt.Sprintf("failed to write in %s", visColl))
	}
	return resp, nil
}

// todo(Aman): Figure out a way to automatically update updatedBy and updatedAt fields. Manually updating it is not scalable.
// MergePartialCg merges partial callgraph of from source branch to dest branch in case corresponding pr is merged
// It also cleans up the nodes which have been deleted in the PR from nodes and relations collections.
func (mdb *MongoDb) MergePartialCg(ctx context.Context, req types.MergePartialCgRequest) error {
	commit := req.Diff.Sha
	branch := req.TargetBranch
	repo := utils.GetRepoUrl(req.Repo)
	files := req.Diff.Files

	// merging nodes
	// get all the nids which are from the dest branch
	f := bson.M{"account": req.AccountId, "vcs_info.branch": branch, "vcs_info.repo": repo}
	dNids, err := mdb.getNodeIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	// get all the nids from the source branch which need to be merged
	f = bson.M{"account": req.AccountId, "vcs_info.commit_id": commit, "vcs_info.repo": repo}
	srcNids, err := mdb.getNodeIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}

	// list of new nodes in src branch
	nodesToMove := utils.GetSliceDiff(srcNids, dNids)
	err = mdb.mergeNodes(ctx, commit, branch, repo, req.AccountId, nodesToMove)
	if err != nil {
		return err
	}

	// merge relations
	// get all the nids which are from the dest branch
	f = bson.M{"vcs_info.branch": branch, "vcs_info.repo": repo, "account": req.AccountId}
	dRelIDs, err := mdb.getTestRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	// get all the nids from the source branch which need to be merged
	f = bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo, "account": req.AccountId}
	sRelIDs, err := mdb.getTestRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	err = mdb.mergeRelations(ctx, commit, branch, repo, req.AccountId, sRelIDs, dRelIDs)
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

	// merge vis_edges
	// get all the nids which are from the dest branch
	f = bson.M{"account": req.AccountId, "vcs_info.repo": repo, "vcs_info.branch": branch}
	dVisIDs, err := mdb.getVgRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	// get all the nids from the source branch which need to be merged
	f = bson.M{"account": req.AccountId, "vcs_info.repo": repo, "vcs_info.commit_id": commit}
	sVisIDs, err := mdb.getVgRelIds(ctx, commit, branch, repo, f)
	if err != nil {
		return err
	}
	err = mdb.mergeVisEdges(ctx, commit, branch, repo, req.AccountId, sVisIDs, dVisIDs)
	if err != nil {
		return err
	}

	// if deleted files are empty, there are no nodes and relations to update
	if len(deletedF) == 0 {
		return nil
	}

	n, err := utils.ParseFileNames(deletedF)
	logger.FromContext(ctx).Infow(fmt.Sprintf("deleted %d files", len(n)),
		"repo", repo,
		"branch", branch,
		"commit", commit,
	)

	// condition for fetching ids of nodes which are deleted
	cond := []interface{}{}
	for _, v := range n {
		cond = append(cond, bson.M{"package": v.Pkg, "class": v.Class, "vcs_info.branch": branch, "vcs_info.repo": repo, "account": req.AccountId})
	}
	f = bson.M{"$or": cond}
	cur, err := mdb.Database.Collection(nodeColl).Find(ctx, f, &options.FindOptions{})
	if err != nil {
		return formatError(err, "failed to query nodes coll for deleted files", repo, branch, commit)
	}
	delIDs := []int{} // delIDs is list of ids of nodes which are deleted
	for cur.Next(ctx) {
		var node Node
		err = cur.Decode(&node)
		if err != nil {
			return formatError(err, "failed to fetch node for deleted files", repo, branch, commit)
		}
		delIDs = append(delIDs, node.Id)
	}
	logger.FromContext(ctx).Infow(fmt.Sprintf("node ids to be deleted: [%v]", delIDs), "branch", branch, "repo", repo)
	if len(delIDs) > 0 {
		// delete nodes with id in delIDs
		f = bson.M{"id": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch, "account": req.AccountId}
		r, err := mdb.Database.Collection(nodeColl).DeleteMany(ctx, f, &options.DeleteOptions{})
		if err != nil {
			return formatError(err, fmt.Sprintf("failed to delete records from nodes coll delIDs: %v", delIDs), repo, branch, commit)
		}

		// delete relations with source in delIDs
		f = bson.M{"source": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch, "account": req.AccountId}
		r1, err := mdb.Database.Collection(relnsColl).DeleteMany(ctx, f, &options.DeleteOptions{})
		if err != nil {
			return formatError(err, fmt.Sprintf("failed to delete records from relns coll delIDs: %v", delIDs), repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("deleted %d, %d records from nodes, relations collection for deleted files",
			r.DeletedCount, r1.DeletedCount), "branch", branch, "repo", repo)

		// update tests fields which contains delIDs in relations
		f = bson.M{"tests": bson.M{"$in": delIDs}, "vcs_info.repo": repo, "vcs_info.branch": branch, "account": req.AccountId}
		update := bson.M{"$pull": bson.M{"tests": bson.M{"$in": delIDs}}}
		res, err := mdb.Database.Collection(relnsColl).UpdateMany(ctx, f, update)
		if err != nil {
			return formatError(err, "failed to get records in relations collection", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("matched %d, updated %d records from relations collection for deleted files",
			res.MatchedCount, res.ModifiedCount), "branch", branch, "repo", repo)

		// delete edges with caller in delIDs
		f = bson.M{"account": req.AccountId, "vcs_info.repo": repo, "vcs_info.branch": branch, "caller": bson.M{"$in": delIDs}}
		r, err = mdb.Database.Collection(visColl).DeleteMany(ctx, f, &options.DeleteOptions{})
		if err != nil {
			return formatError(err, fmt.Sprintf("failed to delete records from vis_edge coll delIDs: %v", delIDs), repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("deleted %d records from vis_edge collection for deleted files",
			r.DeletedCount), "branch", branch, "repo", repo)

		// update callee fields which contains delIDs in vis_edge collection
		f = bson.M{"account": req.AccountId, "vcs_info.repo": repo, "vcs_info.branch": branch, "callee": bson.M{"$in": delIDs}}
		update = bson.M{"$pull": bson.M{"callee": bson.M{"$in": delIDs}}}
		res, err = mdb.Database.Collection(visColl).UpdateMany(ctx, f, update)
		if err != nil {
			return formatError(err, "failed to get records in vis_edge collection", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("matched %d, updated %d records in vis_edge collection for deleted files",
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
func (mdb *MongoDb) getNodeIds(ctx context.Context, commit, branch, repo string, f interface{}) ([]int, error) {
	var nodes []Node
	var nIds []int
	cur, err := mdb.Database.Collection(nodeColl).Find(ctx, f)
	if err != nil {
		return []int{}, formatError(err, "failed in find query in nodes collection", repo, branch, commit)
	}
	if err = cur.All(ctx, &nodes); err != nil {
		return []int{}, formatError(err, "failed to iterate on records using cursor in nodes collection", repo, branch, commit)
	}
	for _, v := range nodes {
		nIds = append(nIds, v.Id)
	}
	return nIds, nil
}

// getRelIds queries mongo and returns list of relation ID's for the given filter
func (mdb *MongoDb) getTestRelIds(ctx context.Context, commit, branch, repo string, f interface{}) ([]int, error) {
	var relIDS []int
	var relations []Relation
	cur, err := mdb.Database.Collection(relnsColl).Find(ctx, f)
	if err != nil {
		return []int{}, formatError(err, "failed in find query in rel collection", repo, branch, commit)
	}
	if err = cur.All(ctx, &relations); err != nil {
		return []int{}, formatError(err, "failed to iterate on records using cursor in relations collection", repo, branch, commit)
	}
	for _, v := range relations {
		relIDS = append(relIDS, v.Source)
	}
	return relIDS, nil
}

// getRelIds queries mongo and returns list of vis_relation ID's for the given filter
func (mdb *MongoDb) getVgRelIds(ctx context.Context, commit, branch, repo string, f interface{}) ([]int, error) {
	var relIDS []int
	var visRelns []VisEdge
	cur, err := mdb.Database.Collection(visColl).Find(ctx, f)
	if err != nil {
		return []int{}, formatError(err, "failed in find query in visEdge collection", repo, branch, commit)
	}
	if err = cur.All(ctx, &visRelns); err != nil {
		return []int{}, formatError(err, "failed to iterate on records using cursor in visEdge collection", repo, branch, commit)
	}
	for _, v := range visRelns {
		relIDS = append(relIDS, v.Caller)
	}
	return relIDS, nil
}

// mergeNodes merges records in nodes collection in case of a pr merge from source branch to destination branch
// #1: Move unique nodes records in src branch to destination branch
// #2; delete all entries in src branch as the merging is complete.
func (mdb *MongoDb) mergeNodes(ctx context.Context, commit, branch, repo, account string, nodesToMove []int) error {
	// update `branch` field of the nodes from source to dest
	if len(nodesToMove) > 0 {
		f := bson.M{"vcs_info.commit_id": commit, "id": bson.M{"$in": nodesToMove}, "vcs_info.repo": repo, "account": account}
		update := bson.M{"$set": bson.M{"vcs_info.branch": branch}}
		res, err := mdb.Database.Collection(nodeColl).UpdateMany(ctx, f, update)
		if err != nil {
			return formatError(err, "failed to merge cg in nodes collection for", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// delete remaining records of src branch from nodes collection
	// todo(AMAN):  find a better filter than $ne
	f := bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo, "account": account, "vcs_info.branch": bson.M{"$ne": branch}}
	res, err := mdb.Database.Collection(nodeColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return formatError(err, "failed to delete records in nodes collection", repo, branch, commit)
	}
	logger.FromContext(ctx).Infow(fmt.Sprintf("marked %d records as expired from nodes collection", res.ModifiedCount),
		"account", account,
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
func (mdb *MongoDb) mergeRelations(ctx context.Context, commit, branch, repo, account string, sIDs []int, dIDs []int) error {
	// list of new relToMove in src branch
	relToMove := utils.GetSliceDiff(sIDs, dIDs)
	// moving relations records
	// update `branch` field of the relToMove from source to dest
	if len(relToMove) > 0 {
		f := bson.M{"vcs_info.commit_id": commit, "source": bson.M{"$in": relToMove}, "vcs_info.repo": repo, "account": account}
		u := bson.M{"$set": bson.M{"vcs_info.branch": branch}}
		res, err := mdb.Database.Collection(relnsColl).UpdateMany(ctx, f, u)
		if err != nil {
			return formatError(err, "failed to merge cg in nodes collection for", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("moving records: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// updating commons records in relations collection in source and destination branches
	var srcRelation, destRelation []Relation
	relIDToUpdate := utils.GetSliceDiff(sIDs, relToMove)
	logger.FromContext(ctx).Infow("updating relations",
		"len(sIDs)", len(sIDs),
		"len(relToMove)", len(relToMove),
		"repo", repo,
		"branch", branch,
		"commit", commit)
	if len(relIDToUpdate) > 0 {
		f := bson.M{"vcs_info.branch": branch, "source": bson.M{"$in": relIDToUpdate}, "vcs_info.repo": repo, "account": account}
		// filter for getting relations from destination branch
		cur, err := mdb.Database.Collection(relnsColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", repo, branch, commit)
		}
		if err = cur.All(ctx, &destRelation); err != nil {
			return formatError(err, "failed to iterate on records using cursor in relations collection", repo, branch, commit)
		}
		// filter for getting relations from source branch
		f = bson.M{"vcs_info.commit_id": commit, "source": bson.M{"$in": relIDToUpdate}, "vcs_info.repo": repo, "account": account}
		cur, err = mdb.Database.Collection(relnsColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", repo, branch, commit)
		}
		if err = cur.All(ctx, &srcRelation); err != nil {
			return formatError(err, "failed to iterate on records using cursor in relations collection", repo, branch, commit)
		}
		destMap := getRelMap(srcRelation, destRelation)
		var operations []mongo.WriteModel
		for src, tests := range destMap {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"source": src, "vcs_info.repo": repo, "vcs_info.branch": branch, "account": account})
			operation.SetUpdate(bson.M{"$set": bson.M{"tests": tests}})
			operations = append(operations, operation)
		}
		res, err := mdb.Database.Collection(relnsColl).BulkWrite(ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to merge relations collection", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("relations merge: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// delete remaining records of src branch from relations collection
	// todo(AMAN):  find a better filter than $ne
	f := bson.M{"vcs_info.commit_id": commit, "vcs_info.repo": repo, "account": account, "vcs_info.branch": bson.M{"$ne": branch}}
	res, err := mdb.Database.Collection(relnsColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return formatError(err, "failed to delete records in relations collection", repo, branch, commit)
	}
	logger.FromContext(ctx).Infow(fmt.Sprintf("marked %d records as deleted from relation collection", res.ModifiedCount),
		"account", account,
		"repo", repo,
		"branch", branch,
		"commit", commit,
	)
	return nil
}

// mergeVisEdges merges records in vis_edges collection in case of a pr merge from source branch to destination branch
// #1: Move unique vis_edge records in src branch to destination branch
// #2: for source which exists in both src and destination branch, merge tests form both and update edges of dest branch
// #3; delete all entries in src branch as the merging is complete.
func (mdb *MongoDb) mergeVisEdges(ctx context.Context, commit, branch, repo, account string, sIDs []int, dIDs []int) error {
	// list of new edges in src branch
	edgesToMove := utils.GetSliceDiff(sIDs, dIDs)
	// moving edge records
	// update `branch` field of the edgeToMove from source to dest
	if len(edgesToMove) > 0 {
		f := bson.M{"account": account, "vcs_info.repo": repo, "vcs_info.commit_id": commit, "caller": bson.M{"$in": edgesToMove}}
		u := bson.M{"$set": bson.M{"vcs_info.branch": branch}}
		res, err := mdb.Database.Collection(visColl).UpdateMany(ctx, f, u)
		if err != nil {
			return formatError(err, "failed to merge vis_edge collection for", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("moving vis_edge records: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// updating commons records in vis_edge collection in source and destination branches
	var srcEdges, destEdges []VisEdge
	edgeIDsToUpdate := utils.GetSliceDiff(sIDs, edgesToMove)
	logger.FromContext(ctx).Infow("updating vis_edges",
		"len(sIDs)", len(sIDs),
		"len(relToMove)", len(edgesToMove),
		"repo", repo,
		"branch", branch,
		"commit", commit)
	if len(edgeIDsToUpdate) > 0 {
		f := bson.M{"account": account, "vcs_info.repo": repo, "vcs_info.branch": branch, "caller": bson.M{"$in": edgeIDsToUpdate}}
		// filter for getting edges from destination branch
		cur, err := mdb.Database.Collection(visColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in vis_edge collection", repo, branch, commit)
		}
		if err = cur.All(ctx, &destEdges); err != nil {
			return formatError(err, "failed to iterate on records using cursor in vis_edges collection", repo, branch, commit)
		}
		// filter for getting edges from source branch
		f = bson.M{"vcs_info.commit_id": commit, "caller": bson.M{"$in": edgeIDsToUpdate}, "vcs_info.repo": repo, "account": account}
		cur, err = mdb.Database.Collection(visColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in vis_edges collection", repo, branch, commit)
		}
		if err = cur.All(ctx, &srcEdges); err != nil {
			return formatError(err, "failed to iterate on records using cursor in vis_edges collection", repo, branch, commit)
		}
		destMap := getVisMap(srcEdges, destEdges)
		var operations []mongo.WriteModel
		for src, dest := range destMap {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"account": account, "vcs_info.repo": repo, "vcs_info.branch": branch, "caller": src})
			operation.SetUpdate(bson.M{"$set": bson.M{"callee": dest}})
			operations = append(operations, operation)
		}
		res, err := mdb.Database.Collection(visColl).BulkWrite(ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to merge vis_edges collection", repo, branch, commit)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("edges merged: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", repo,
			"branch", branch,
			"commit", commit,
		)
	}

	// delete remaining records of src branch from vis_edges collection
	f := bson.M{"account": account, "vcs_info.repo": repo, "vcs_info.commit_id": commit, "vcs_info.branch": bson.M{"$ne": branch}}
	res, err := mdb.Database.Collection(relnsColl).UpdateMany(ctx, f, expireQuery)
	if err != nil {
		return formatError(err, "failed to delete records in vis_edges collection", repo, branch, commit)
	}
	logger.FromContext(ctx).Infow(fmt.Sprintf("marked %d records as deleted from vis_edges collection", res.ModifiedCount),
		"account", account,
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
func (mdb *MongoDb) upsertNodes(ctx context.Context, nodes []Node, info VCSInfo, account string) error {
	logger.FromContext(ctx).Infow("uploading partialcg in nodes collection",
		"#nodes", len(nodes), "repo", info.Repo, "branch", info.Branch, "account", account)
	// fetch existing records for branch
	f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.commit_id": info.CommitId, "vcs_info.repo": info.Repo, "account": account}
	NIds, err := mdb.getNodeIds(ctx, info.CommitId, info.Branch, info.Repo, f)
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
		res, err := mdb.Database.Collection(nodeColl).InsertMany(ctx, nodesToAdd)
		if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("failed to add nodes while uploading partial cg, repo: %s, branch: %s", info.Repo, info.Branch))
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("inserted %d records in nodes collection", len(res.InsertedIDs)),
			"account", account,
			"repo", info.Repo,
			"branch", info.Branch,
		)
	}
	return nil
}

// upsertTestRelations is used to upload partial callagraph to db. If there is already a cg present with
// the same commit, it udpdates that callgraph otherwise creates a new entry The algo for that is:
// 1. get all the existing relations for `repo` + `branch` + `commit_id`.
// 2. Relations received in cg which are new will be inserted in relations collection.
// relations which are already present in the db needs to be merged.
func (mdb *MongoDb) upsertTestRelations(ctx context.Context, relns []Relation, info VCSInfo, account string) error {
	logger.FromContext(ctx).Infow("uploading partialcg in relations collection",
		"#relns", len(relns), "repo", info.Repo, "branch", info.Branch)
	// fetch existing records for branch
	f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.commit_id": info.CommitId, "vcs_info.repo": info.Repo, "account": account}
	Ids, err := mdb.getTestRelIds(ctx, info.CommitId, info.Branch, info.Repo, f)
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
		res, err := mdb.Database.Collection(relnsColl).InsertMany(ctx, relToAdd)
		if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("failed to add relns while uploading partial cg, repo: %s, branch: %s", info.Repo, info.Branch))
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("inserted %d records in relns collection", len(res.InsertedIDs)),
			"account", account,
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
		f = bson.M{"vcs_info.branch": info.Branch, "vcs_info.repo": info.Repo, "account": account, "source": bson.M{"$in": idToUpdate}}
		cur, err := mdb.Database.Collection(relnsColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", info.Repo, info.Repo, info.CommitId)
		}
		if err = cur.All(ctx, &existingRelns); err != nil {
			return formatError(err, "failed to iterate on records using cursor", info.Repo, info.Branch, info.CommitId)
		}
		finalRelations := getRelMap(relToUpdate, existingRelns)
		var operations []mongo.WriteModel
		for src, tests := range finalRelations {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"source": src, "vcs_info.repo": info.Repo, "account": account, "vcs_info.branch": info.Branch})
			operation.SetUpdate(bson.M{"$set": bson.M{"tests": tests}})
			operations = append(operations, operation)
		}
		res, err := mdb.Database.Collection(relnsColl).BulkWrite(ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to update relations collection", info.Repo, info.Branch, info.CommitId)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("relations updated: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
			"repo", info.Repo,
			"branch", info.Branch,
			"commit", info.CommitId,
		)
	}
	return nil
}

// upsertVisRelations is used to upload partial visgraph to db. If there is already a cg present with
// the same commit, it updates that visgraph otherwise creates a new entry The algo for that is:
// 1. get all the existing relations for `repo` + `branch` + `commit_id`.
// 2. Relations received in vg which are new will be inserted in relations collection.
// relations which are already present in the db needs to be merged.
func (mdb *MongoDb) upsertVisRelations(ctx context.Context, relns []VisEdge, info VCSInfo, account string) error {
	logger.FromContext(ctx).Infow("uploading partialcg in vis_edge collection",
		"#relns", len(relns), "repo", info.Repo, "branch", info.Branch)
	// fetch existing records for branch
	f := bson.M{"vcs_info.branch": info.Branch, "vcs_info.commit_id": info.CommitId, "vcs_info.repo": info.Repo, "account": account}
	Ids, err := mdb.getVgRelIds(ctx, info.CommitId, info.Branch, info.Repo, f)
	if err != nil {
		return err
	}
	existingRel := getMap(Ids)
	relToAdd := make([]interface{}, 0)
	relToUpdate := make([]VisEdge, 0)
	for _, rel := range relns {
		if existingRel[rel.Caller] {
			relToUpdate = append(relToUpdate, rel)
		} else {
			relToAdd = append(relToAdd, rel)
		}
	}
	if len(relToAdd) > 0 {
		res, err := mdb.Database.Collection(visColl).InsertMany(ctx, relToAdd)
		if err != nil {
			return errors.Wrap(
				err,
				fmt.Sprintf("failed to add relns while uploading partial cg, repo: %s, branch: %s", info.Repo, info.Branch))
		}
		logger.FromContext(ctx).Infow(fmt.Sprintf("inserted %d records in vis_edge collection", len(res.InsertedIDs)),
			"account", account,
			"repo", info.Repo,
			"branch", info.Branch,
		)
	}
	if len(relToUpdate) > 0 {
		var idToUpdate []int
		var existingRelns []VisEdge
		for _, rel := range relToUpdate {
			idToUpdate = append(idToUpdate, rel.Caller)
		}
		f = bson.M{"vcs_info.branch": info.Branch, "vcs_info.repo": info.Repo, "account": account, "caller": bson.M{"$in": idToUpdate}}
		cur, err := mdb.Database.Collection(visColl).Find(ctx, f)
		if err != nil {
			return formatError(err, "failed in find query in rel collection", info.Repo, info.Repo, info.CommitId)
		}
		if err = cur.All(ctx, &existingRelns); err != nil {
			return formatError(err, "failed to iterate on records using cursor", info.Repo, info.Branch, info.CommitId)
		}
		finalRelations := getVisMap(relToUpdate, existingRelns)
		var operations []mongo.WriteModel
		for src, tests := range finalRelations {
			operation := mongo.NewUpdateOneModel()
			operation.SetFilter(bson.M{"caller": src, "vcs_info.repo": info.Repo, "account": account, "vcs_info.branch": info.Branch})
			operation.SetUpdate(bson.M{"$set": bson.M{"callee": tests}})
			operations = append(operations, operation)
		}
		res, err := mdb.Database.Collection(visColl).BulkWrite(ctx, operations, &options.BulkWriteOptions{})
		if err != nil {
			return formatError(err, "failed to update relations collection", info.Repo, info.Branch, info.CommitId)
		}
		logger.FromContext(ctx).Infow(
			fmt.Sprintf("relations updated: matched %d, updated %d records", res.MatchedCount, res.ModifiedCount),
			"account", account,
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

// getRelMap takes two VisEdge records A and B and returns a map[source]dest object
// where dest is the union of tests of A and B for each entry of A
func getVisMap(src []VisEdge, dest []VisEdge) map[int][]int {
	srcMap := make(map[int][]int)
	destMap := make(map[int][]int)
	for _, relation := range src {
		srcMap[relation.Caller] = relation.Callee
	}
	for _, v := range dest {
		destMap[v.Caller] = v.Callee
	}
	for k, v := range destMap {
		destMap[k] = merge(v, srcMap[k])
	}
	return destMap
}
