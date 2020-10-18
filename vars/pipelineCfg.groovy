def call() {
  Map pipelineCfg = readYaml file: "PipelineCfg.yaml"
  return pipelineCfg
}