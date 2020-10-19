这是一个Jenkins
Jenkins 演示地址：http://devopsdemo.funplus.social/
Jenkins 用户名amdin
jenkins 密码qweqweqwe


共享库使用方法

规则规定：
1. 在master分支上定义两个文件分别为CI_Jenkinsfile、CD_Jenkinsfile与PipelineCfg.yaml（注意大小写）如下图所示
2. 打tag即发版，所有构建均一次build。
CI_Jenkinsfile为固定写法
```
#!groovy   
library 'share-libs'
runCIPipeline()
```
CD_Jenkinsfile为固定写法
```
#!groovy   
library 'share-libs'
runCDPipeline()
```
PipelinCfg.yaml 为配置会根据选用的CI_TYPE、CD_TYPE不同做区分，目前只有一个DEMO选项