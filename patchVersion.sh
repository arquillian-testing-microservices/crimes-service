
#!/bin/sh

var=$(jq -n --arg v "crimes:$1" '[{"op": "replace", "path":"/spec/template/spec/containers/0/image","value": $v}]')

oc patch dc crimes --type=json -p="${var}"
#oc rollout latest dc/crimes -n villains