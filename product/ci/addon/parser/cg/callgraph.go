// This file has a Callgraph object which is used to upload callgraph from
// addon to ti server. It also contains helper functions, FromStringMap which
// takes avro decoded output and returns a Callgraph function. ToStringMap fn
// takes a callgraph object as input and converts it in map[string]interface{} format
// for avro encoding.

// TODO: (Vistaar) Add UT for ensuring avro formatting is correct
// Any changes made to the avro schema needs to be properly validated. This can be done
// using a avro validator online. Also, the schema needs to be converted to a go file
// using a tool like go-bindata.
// go-bindata -o callgraph.go callgraph.avsc
// Without this, none of the changes will take effect.

package cg

import (
	"errors"
	"fmt"
)

// Callgraph object is used for data transfer b/w ti service and lite-engine
type Callgraph struct {
	Nodes     []Node
	Relations []Relation
}

//ToStringMap converts Callgraph to map[string]interface{} for encoding
func (cg *Callgraph) ToStringMap() map[string]interface{} {
	var nodes, relations []interface{}
	for _, v := range (*cg).Nodes {
		data := map[string]interface{}{
			"package":         v.Package,
			"method":          v.Method,
			"id":              v.ID,
			"params":          v.Params,
			"class":           v.Class,
			"type":            v.Type,
			"callsReflection": v.CallsReflection,
			"file":            v.File,
		}
		nodes = append(nodes, data)
	}
	for _, v := range (*cg).Relations {
		data := map[string]interface{}{
			"source": v.Source,
			"tests":  v.Tests,
		}
		relations = append(relations, data)
	}
	data := map[string]interface{}{
		"nodes":     nodes,
		"relations": relations,
	}
	return data
}

//FromStringMap creates Callgraph object from map[string]interface{}
func FromStringMap(data map[string]interface{}) (*Callgraph, error) {
	var fNodes []Node
	var fRel []Relation
	for k, v := range data {
		switch k {
		case "nodes":
			if nodes, ok := v.([]interface{}); ok {
				for _, t := range nodes {
					fields := t.(map[string]interface{})
					var node Node
					for f, v := range fields {
						switch f {
						case "method":
							node.Method = v.(string)
						case "package":
							node.Package = v.(string)
						case "id":
							node.ID = int(v.(int32))
						case "params":
							node.Params = v.(string)
						case "class":
							node.Class = v.(string)
						case "callsReflection":
							node.CallsReflection = v.(bool)
						case "type":
							node.Type = v.(string)
						case "file":
							node.File = v.(string)
						default:
							return nil, errors.New(fmt.Sprintf("unknown field received: %s", f))
						}
					}
					fNodes = append(fNodes, node)
				}
			} else {
				return nil, errors.New("failed to parse nodes in callgraph")
			}
		case "relations":
			if relns, ok := v.([]interface{}); ok {
				for _, reln := range relns {
					var relation Relation
					fields := reln.(map[string]interface{})
					for k, v := range fields {
						switch k {
						case "source":
							relation.Source = int(v.(int32))
						case "tests":
							var testsN []int
							for _, v := range v.([]interface{}) {
								testsN = append(testsN, int(v.(int32)))
							}
							relation.Tests = testsN
						default:
							return nil, errors.New(fmt.Sprintf("unknown field received: %s", k))
						}
					}
					fRel = append(fRel, relation)
				}
			} else {
				return nil, errors.New("failed to parse relns in callgraph")
			}
		}
	}
	return &Callgraph{
		Relations: fRel,
		Nodes:     fNodes,
	}, nil
}

//Node type represents detail of node in callgraph
type Node struct {
	Package         string
	Method          string
	ID              int
	Params          string
	Class           string
	Type            string
	CallsReflection bool
	File            string
}

// Input is the go representation of each line in callgraph file
type Input struct {
	Test     Node
	Source   Node
	Resource Node
}

//Relation b/w source and test
type Relation struct {
	Source int
	Tests  []int
}

// TODO -- not required. Delete it as now visgraph format only contains id's instead of node information
func NewNode(id int, typ, pkg, class, method, params string) *Node {
	return &Node{
		Package: pkg,
		Method:  method,
		ID:      id,
		Params:  params,
		Class:   class,
		Type:    typ,
	}
}
