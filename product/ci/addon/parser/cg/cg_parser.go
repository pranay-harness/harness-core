// This file implements a parser for callgraph file. It reads all the files
// in the callgraph directory, dedupes the data, and then returns Callgraph object
package cg

import (
	"bufio"
	"encoding/json"
	"fmt"
	"go.uber.org/zap"

	"github.com/pkg/errors"
	"github.com/wings-software/portal/commons/go/lib/filesystem"
)

//Parser reads callgraph file, processes it to extract
// nodes and relations
type Parser interface {
	// Parse and read the file from
	Parse(file []string) (*Callgraph, error)
}

// CallGraphParser struct definition
type CallGraphParser struct {
	log *zap.SugaredLogger    // logger
	fs  filesystem.FileSystem // filesystem handler
}

// NewCallGraphParser creates a new CallGraphParser client and returns it
func NewCallGraphParser(log *zap.SugaredLogger, fs filesystem.FileSystem) *CallGraphParser {
	return &CallGraphParser{
		log: log,
		fs:  fs,
	}
}

// iterate through all the cg files in the directory, parse each of them and return Callgraph object
func (cg *CallGraphParser) Parse(files []string) (*Callgraph, error) {
	var finalCg []string
	for _, file := range files {
		f, err := cg.fs.Open(file)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("failed to open file %s", file))
		}
		r := bufio.NewReader(f)
		cgStr, err := rFile(r)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("failed to parse file %s", file))
		}
		cg.log.Infow(fmt.Sprintf("successfully parsed cg file %s", file))
		finalCg = append(finalCg, cgStr...)
	}
	return parseInt(finalCg)
}

// parseInt reads the input callgraph file and converts it into callgraph object
func parseInt(cgStr []string) (*Callgraph, error) {
	var (
		err error
		inp []Input
	)
	for _, line := range cgStr {
		tinp := &Input{}
		err = json.Unmarshal([]byte(line), tinp)
		if err != nil {
			return nil, errors.Wrap(err, fmt.Sprintf("data unmarshalling to json failed for line [%s]", line))
		}
		inp = append(inp, *tinp)
	}
	return process(inp), nil
}

func process(inps []Input) *Callgraph {
	var relns []Relation
	var nodes []Node

	relMap, nodeMap := convCgph(inps)
	// Updating the Relations map
	for k, v := range relMap {
		tRel := Relation{
			Source: k,
			Tests:  v,
		}
		relns = append(relns, tRel)
	}

	// Updating the nodes map
	for _, v := range nodeMap {
		nodes = append(nodes, v)
	}
	return &Callgraph{
		Nodes:     nodes,
		Relations: relns,
	}
}

func convCgph(inps []Input) (map[int][]int, map[int]Node) {
	relMap := make(map[int][]int)
	nodeMap := make(map[int]Node)

	for _, inp := range inps {
		// processing nodeMap
		test := inp.Test
		test.Type = "test"
		testID := test.ID
		nodeMap[testID] = test
		// processing relmap
		var source Node
		if inp.Source == (Node{}) {
			source = inp.Resource
			source.Type = "resource"
		} else {
			source = inp.Source
			source.Type = "source"
		}
		sourceID := source.ID
		nodeMap[sourceID] = source
		relMap[sourceID] = append(relMap[sourceID], testID)
	}
	return relMap, nodeMap
}

// rLine reads line in callgraph file which corresponds to one entry of callgraph
// had to use bufio reader as the scanner.Scan() fn has limitation
// over the number of bytes it can read and was not working on callgraph file.
func rLine(r *bufio.Reader) (string, error) {
	var (
		isPrefix bool  = true
		err      error = nil
		line, ln []byte
	)
	for isPrefix && err == nil {
		line, isPrefix, err = r.ReadLine()
		ln = append(ln, line...)
	}
	return string(ln), err
}

// rFile reads callgraph file
func rFile(r *bufio.Reader) ([]string, error) {
	var ret []string
	s, e := rLine(r)
	for e == nil {
		ret = append(ret, s)
		s, e = rLine(r)
	}
	return ret, nil
}
