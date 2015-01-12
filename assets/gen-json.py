#!/usr/bin/env python
import json
import os
import string

template = json.load(open("templates/template-kubernetes.json",'r'))
config = json.load(open('config.json.tmp','r'))

def mapValues(lines):
    for index, line in enumerate(lines):
        for pattern, repl in map.items():
            line = string.replace(line, pattern, repl)
        lines[index] = line
    return lines

params = dict()
for item in config:
  key = item.get('ParameterKey')
  val = item.get('ParameterValue')
  params[key] = val

map = {
    "$DiscoveryURL": params.get('DiscoveryURL')
}

with open('./cloud-init/master.yaml','r') as f:
  master = f.readlines()
  master = mapValues(master)

with open('./cloud-init/minion.yaml','r') as f:
  minion = f.readlines()
  minion = mapValues(minion)

template['Resources']['KubernetesMasterServerLaunchConfig']['Properties']['UserData']['Fn::Base64']['Fn::Join'] = [ '', master ]
template['Resources']['KubernetesMinionServerLaunchConfig']['Properties']['UserData']['Fn::Base64']['Fn::Join'] = [ '', minion ]
template['Parameters']['ClusterSize']['Default'] = str(os.getenv('KUBERNETES_MINION_INSTANCES', 3))

print json.dumps(template)
