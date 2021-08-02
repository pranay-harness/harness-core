package visualization

import (
	"fmt"
	"github.com/pkg/errors"

	cgp "github.com/wings-software/portal/product/ci/addon/parser/cg"
)

// Visgraph object is contains visualization data for callgraph
type Visgraph struct {
	Keys   []cgp.Node
	Values []cgp.Node
}

// Input is the go representation of each line in visgraph file
type Input struct {
	Test   cgp.Node
	Source cgp.Node
}

//ToStringMap converts Visgraph to map[string]interface{} for encoding
func (vg *Visgraph) ToStringMap() map[string]interface{} {
	var keys, values []interface{}
	for _, v := range (*vg).Keys {
		data := map[string]interface{}{
			"package": v.Package,
			"method":  v.Method,
			"id":      v.ID,
			"params":  v.Params,
			"class":   v.Class,
			"type":    v.Type,
		}
		keys = append(keys, data)
	}
	for _, v := range (*vg).Values {
		data := map[string]interface{}{
			"package": v.Package,
			"method":  v.Method,
			"id":      v.ID,
			"params":  v.Params,
			"class":   v.Class,
			"type":    v.Type,
		}
		values = append(values, data)
	}
	data := map[string]interface{}{
		"keys":   keys,
		"values": values,
	}
	return data
}

//FromStringMap creates vis graph object from map[string]interface{}
func FromStringMap(data map[string]interface{}) (*Visgraph, error) {
	var fKeys []cgp.Node
	var fValues []cgp.Node
	var err error
	for k, v := range data {
		switch k {
		case "keys":
			if nodes, ok := v.([]interface{}); ok {
				fKeys, err = parseNode(nodes, fKeys)
				if err != nil {
					return nil, errors.Wrap(err, "failed to convert string to node")
				}
			} else {
				return nil, errors.New("failed to parse keys in visgraph")
			}
		case "values":
			if nodes, ok := v.([]interface{}); ok {
				fKeys, err = parseNode(nodes, fKeys)
				if err != nil {
					return nil, errors.Wrap(err, "failed to convert string to node")
				}
			} else {
				return nil, errors.New("failed to parse values in visgraph")
			}
		}
	}
	return &Visgraph{
		Keys:   fKeys,
		Values: fValues,
	}, nil
}

func parseNode(nodes []interface{}, fKeys []cgp.Node) ([]cgp.Node, error) {
	for _, t := range nodes {
		fields := t.(map[string]interface{})
		var node cgp.Node
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
			case "type":
				node.Type = v.(string)
			default:
				return nil, errors.New(fmt.Sprintf("unknown field received: %s", f))
			}
		}
		fKeys = append(fKeys, node)
	}
	return fKeys, nil
}
