package ti

import (
	"reflect"
	"testing"

	"github.com/stretchr/testify/assert"
)

func TestCallgraph_ToStringMap(t *testing.T) {
	cg := Callgraph{
		Nodes: []Node{
			{
				Package: "package1",
				Method:  "m1",
				ID:      1,
				Params:  "param1",
				Class:   "class1",
				Type:    "source",
			},
			{
				Package:         "package2",
				Method:          "m2",
				ID:              1,
				Params:          "param2",
				Class:           "class2",
				Type:            "test",
				CallsReflection: true,
			},
		},
		Relations: []Relation{
			{
				Source: 0,
				Tests:  []int{1, 2, 3, 4, 5},
			},
			{
				Source: 1,
				Tests:  []int{11, 12, 13, 14, 15},
			},
		},
	}
	mp := cg.ToStringMap()

	fNodes, fRelations := getCgObject(mp)
	finalCg := Callgraph{
		Nodes:     fNodes,
		Relations: fRelations,
	}
	assert.Equal(t, reflect.DeepEqual(finalCg, cg), true)
}

func getCgObject(mp map[string]interface{}) ([]Node, []Relation) {
	var fNodes []Node
	var fRelations []Relation
	for k, v := range mp {
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
							node.ID = v.(int)
						case "params":
							node.Params = v.(string)
						case "class":
							node.Class = v.(string)
						case "callsReflection":
							node.CallsReflection = v.(bool)
						case "type":
							node.Type = v.(string)
						}
					}
					fNodes = append(fNodes, node)
				}
			}
		case "relations":
			if relations, ok := v.([]interface{}); ok {
				for _, reln := range relations {
					var relation Relation
					fields := reln.(map[string]interface{})
					for k, v := range fields {
						switch k {
						case "source":
							relation.Source = v.(int)
						case "tests":
							var testsN []int
							for _, v := range v.([]int) {
								testsN = append(testsN, v)
							}
							relation.Tests = testsN
						}
					}
					fRelations = append(fRelations, relation)
				}
			}
		}
	}
	return fNodes, fRelations
}
