#!groovy
def call(cfg) {
    // 导入封装的方法
    def method = new org.encapsulation.method()
    // 导入docker模块
    def docker = new org.devops.docker()
    // 导入sonar模块
    def sonar = new org.devops.sonar()
    // 导入通用模块
    def anothertool = new org.devops.anothertool()

    // 导入默认配置
    method.defaultConf()

    // 不能注释否则ci拿不到参数
    try{
        branchName = anothertool.GetBranch("${pushCategory}","${branch}")
        anothertool.PrintMes("CI BEGING","green")
    } catch (Exception e) {
        def runOpts = "manaul"
        anothertool.PrintMes("Read SETTINGS","green")
    }


    pipeline {
        agent {
            kubernetes {
                label "demo"
            }
        }


        // jenkins 配置设置
        options {
            timestamps()  //日志会有时间
            skipDefaultCheckout()  //删除隐式checkout scm语句
            // disableConcurrentBuilds() //禁止并行
            timeout(time: 1, unit: 'HOURS')  //流水线超时设置1h
        }
        

        // 参数设置
        // parameters {
        // }

        triggers {
            GenericTrigger(
                genericVariables: [
                // 触发类型分BRANCH 与 tag
                [defaultValue: 'NOMSG',key: 'pushCategory', value: '$.object_kind'],
                // 触发分支名称
                [defaultValue: 'NOMSG',key: 'BRANCH', value: '$.ref'],
                // 修改代码的用户名
                [defaultValue: 'NOMSG',key: 'USERNAME', value: '$.user_username'],
                // 发版地址
                [defaultValue: 'NOMSG',key: 'url', value: '$.project.web_url'],
                // 删除tag分支
                [defaultValue: 'NOMSG',key: 'after', value: '$.after'],
                // 项目名称
                [defaultValue: 'NOMSG',key: 'project', value: '$.project.name'],
                // Commit项目名称
                [defaultValue: 'NOMSG',key: 'COMMMIT_MESSAGE', value: '$.commits[-1].message'],
                // GitlabGroup名称
                [defaultValue: 'NOMSG',key: 'GITLAB_GROUP', value: '$.project.namespace'],
                ],

                genericRequestVariables: [
                [key: "runOpts"]
                ],	

                causeString: 'Generic Cause',

                token: JOB_NAME,

                printContributedVariables: true,
                printPostContent: true,
                silentResponse: true,

                regexpFilterExpression: '(CI|Ci|cI|ci):.*',
                regexpFilterText: '$COMMMIT_MESSAGE'

            )
        }

        // 常量参数，初始确定后一般不需更改
        environment {

        }

        post {
            success {
                script{
                    currentBuild.description = "\n 构建成功!" 
                    // method.notice("success","${cfg.CI_ROBOT_ID}","${JOB_NAME}","${JOB_URL}","${BUILD_USER}","build success")
                }
            }


            failure {
                script{
                    currentBuild.description = "\n 构建失败!" 
                    // method.notice("error","${cfg.CI_ROBOT_ID}","${JOB_NAME}","${JOB_URL}","${BUILD_USER}","build faild")
                }
            }
            
            aborted {
                script{
                    currentBuild.description = "\n 构建取消!" 
                }
            }

            // always {
            //     cleanWs()
            // }
        }

        //步骤设置
        stages {
            stage('CI获取版本库代码') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    script{
                        // dir("${JOB_NAME}-${BUILD_NUMBER}"){
                        // 导入配置文件
                        anothertool.PrintMes("CI CODE PULL BEGIN","green")
                        method.gitscm("${branchName}","${url}")
                        // 获取最后一条commit 信息的提交
                        COMMMIT_MESSAGE = "${COMMMIT_MESSAGE}".split('\n')[0].split(':')[1]
                        // 判断如果文件夹不存在，退出并告警
                        sh "[ -d $COMMMIT_MESSAGE ] || return"
                        anothertool.PrintMes("CI CODE PULL OVER","green")
                        // }
                        // 覆盖配置文件
                        cfg = pipelineCfg()
                        println cfg
                    }
                }
            }

            stage('编译代码') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    container('nodejs'){
                        script{
                            // dir("${JOB_NAME}-${BUILD_NUMBER}"){
                            // 导入配置文件
                            anothertool.PrintMes("CI CODE BUILD BEGIN","green")
                            sh """
                            npm install
                            npm run build:prod
                            docker build -t nodedemo:v1.0.0 .
                            """
                            anothertool.PrintMes("CI CODE BUILD OVER","green")
                            // }
                        }
                    }
                }
            }

            stage('推送docker') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    container('docker') {
                        script{
                            // dir("${JOB_NAME}-${BUILD_NUMBER}"){
                            anothertool.PrintMes("----------------","green")
                            dir("${COMMMIT_MESSAGE}"){
                                // sh "sed -i 's/default/${COMMMIT_MESSAGE}-${JAR_TAG}/g' startup.sh"
                                sh "sed -i 's/default/${COMMMIT_MESSAGE}-${JAR_TAG}/g' Dockerfile"
                                docker.PushHarborDocker("${COMMMIT_MESSAGE}","${DOCKER_IMAGE_TAG}","${HARBOR_URL}","${GITLAB_GROUP}")
                            }
                            // }
                        }
                    }
                }
            }

            stage('打包推送到helm') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    container("${cfg.CI_EKS_NAME}") {
                        script{
                            // dir("${JOB_NAME}-${BUILD_NUMBER}/${COMMMIT_MESSAGE}/deployment/helm/"){
                            dir("${COMMMIT_MESSAGE}/deployment/helm/"){
                                withCredentials([usernamePassword(credentialsId: 'harbor-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
                                    sh """
                                    HELM_PACKAGE_NAME=`helm package ${COMMMIT_MESSAGE}| awk -F '/' '{print \$NF}'`
                                    helm repo add --username=${username} --password=${password}  ${COMMMIT_MESSAGE} https://${HARBOR_URL}/chartrepo/${GITLAB_GROUP}
                                    helm push \$HELM_PACKAGE_NAME ${COMMMIT_MESSAGE}
                                    """
                                }
                            }
                        }
                    }
                }
            }          
        }
    }
}