package org.encapsulation

def gitscm(branchName,url){
    checkout([$class: 'GitSCM', branches: [[name: branchName]],
    doGenerateSubmoduleConfigurations: false,
    extensions: [
        [$class: 'SubmoduleOption', disableSubmodules: false, parentCredentials: true, recursiveSubmodules: true, reference: '', trackingSubmodules: true]
        ], submoduleCfg: [],
    userRemoteConfigs: [[credentialsId: CRED_ID, url: url]]])
}

def notice(state,ROBOT_ID,JOB_NAME,JOB_URL,BUILD_USER,MESSAGE_INFO){
    // 判断状态
    if (state == "success"){
        res = "- 状态：<font color=#52C41A>${currentBuild.currentResult}</font>"
    } else if (state == "error"){
        res = "- 状态：<font color=#F5222D>${currentBuild.currentResult}</font>"
    }
    // 日志路径替换
    JOB_URL = JOB_URL.replaceAll("http://jenkins:8080","https://jenkins.inf.funplus.social")
    BUILD_URL = BUILD_URL.replaceAll("http://jenkins:8080","https://jenkins.inf.funplus.social")

    // 钉钉插件
    JOB_URL = 
    dingTalk (
        robot: ROBOT_ID,
        type: 'ACTION_CARD',
        title: 'Deploy',
        text: [
            "[${JOB_NAME}](${JOB_URL})\n",
            '---',
            "- 任务：[#${BUILD_ID}](${BUILD_URL}/console)",
            "${res}",
            "- 执行人：${BUILD_USER}",
            "- 持续时间：${currentBuild.durationString}",
            "- 消息：${MESSAGE_INFO}",
            "- 私有仓库地址： [点我](http://nexus.inf.funplus.social/#browse/browse:android-social-platform-snapshot)\n"
        ],
        btns: [
        [
            title: 'Sonar',
            actionUrl: "https://sonarqube.ftsview.com/"
        ],
        [
            title: '私有仓库',
            actionUrl: "http://nexus.inf.funplus.social/#browse/browse:android-social-platform-snapshot"
        ]
    ])
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