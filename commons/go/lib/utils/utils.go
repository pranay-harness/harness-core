package utils

import (
	"regexp"
	"strings"
	"time"
)

type NodeType int32

const (
	NodeType_SOURCE   NodeType = 0
	NodeType_TEST     NodeType = 1
	NodeType_CONF     NodeType = 2
	NodeType_RESOURCE NodeType = 3
	NodeType_OTHER    NodeType = 4
)

type LangType int32

const (
	LangType_JAVA    LangType = 0
	LangType_GO      LangType = 1
	LangType_PYTHON  LangType = 2
	LangType_UNKNOWN LangType = 3
)

const (
	JAVA_SRC_PATH  = "src/main/java/"
	JAVA_TEST_PATH = "src/test/java/"
)

//Node holds data about a source code
type Node struct {
	Pkg    string
	Class  string
	Method string
	Lang   LangType
	Type   NodeType
}

//TimeSince returns the number of milliseconds that have passed since the given time
func TimeSince(t time.Time) float64 {
	return Ms(time.Since(t))
}

//Ms returns the duration in millisecond
func Ms(d time.Duration) float64 {
	return float64(d) / float64(time.Millisecond)
}

//NoOp is a basic NoOp function
func NoOp() error {
	return nil
}

// IsTest checks whether the parsed node is of a test type or not.
func IsTest(node Node) bool {
	return node.Type == NodeType_TEST
}

// IsSupported checks whether we can perform an action for the node type or not.
func IsSupported(node Node) bool {
	return node.Type == NodeType_TEST || node.Type == NodeType_SOURCE
}

//ParseJavaNode extracts the pkg and class names from a Java file path
// e.g., 320-ci-execution/src/main/java/io/harness/stateutils/buildstate/ConnectorUtils.java
// will return pkg = io.harness.stateutils.buildstate, class = ConnectorUtils
func ParseJavaNode(filename string) (*Node, error) {
	var node Node
	node.Pkg = ""
	node.Class = ""
	node.Lang = LangType_UNKNOWN
	node.Type = NodeType_OTHER

	filename = strings.TrimSpace(filename)
	if !strings.HasSuffix(filename, ".java") {
		return &node, nil
	}
	node.Lang = LangType_JAVA

	var r *regexp.Regexp
	if strings.Contains(filename, JAVA_SRC_PATH) {
		r = regexp.MustCompile(`^.*src/main/java/`)
		node.Type = NodeType_SOURCE
	} else if strings.Contains(filename, JAVA_TEST_PATH) {
		r = regexp.MustCompile(`^.*src/test/java/`)
		node.Type = NodeType_TEST
	} else {
		return &node, nil
	}
	rr := r.ReplaceAllString(filename, "${1}") //extract the 2nd part after matching the src/test prefix
	rr = strings.TrimSuffix(rr, ".java")

	parts := strings.Split(rr, "/")
	p := parts[:len(parts)-1]
	node.Class = parts[len(parts)-1]
	node.Pkg = strings.Join(p, ".")

	return &node, nil
}

//ParseFileNames accepts a list of file names, parses and returns the list of Node
func ParseFileNames(files []string) ([]Node, error) {

	nodes := make([]Node, 0)
	for _, path := range files {
		if len(path) == 0 {
			continue
		}
		//TODO(shiv): right now, defaulting to Java, add support for additional languages
		node, _ := ParseJavaNode(path)
		nodes = append(nodes, *node)
	}
	return nodes, nil
}

// GetSliceDiff returns the unique element in sIDs which are not present in dIDs
func GetSliceDiff(sIDs []int, dIDs []int) []int {
	mp := make(map[int]bool)
	var ret []int
	for _, id := range dIDs {
		mp[id] = true
	}
	for _, id := range sIDs {
		if _, ok := mp[id]; !ok {
			ret = append(ret, id)
		}
	}
	return ret
}

// GetRepoUrl takes the repo address and appends .git at the end if it doesn't ends with .git
// TODO: Check if this works for SSH access
func GetRepoUrl(repo string) string {
	if !strings.HasSuffix(repo, ".git") {
		repo += ".git"
	}
	return repo
}
