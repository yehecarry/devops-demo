package org.encapsulation

def gitscm(branchName,url){
    checkout([$class: 'GitSCM', branches: [[name: branchName]],
    doGenerateSubmoduleConfigurations: false,
    extensions: [
        [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: true]
        ], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: CRED_ID, url: url]]])
}


def defaultConf(){
    // 定义配置文件
    def branchName 
    def runTests 
    def testCommand
    def runCheck 
    def checkCommand
    def runSonar 
    def runCd 
    // EKS 配置
}


return this