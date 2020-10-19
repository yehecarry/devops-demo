#!groovy
def call(cfg) {
    // 导入封装的方法
    def method = new org.encapsulation.method()
    // 导入docker模块
    def docker = new org.devops.docker()
    // 导入sonar模块
    // def sonar = new org.devops.sonar()
    // 导入通用模块
    def anothertool = new org.devops.anothertool()

    // 导入默认配置
    method.defaultConf()

    branchName = anothertool.GetBranch("${pushCategory}","${branch}")
        


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
                [defaultValue: 'NOMSG',key: 'PROJECT', value: '$.project.name'],
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

                // regexpFilterExpression: '(CI|Ci|cI|ci):.*',
                // regexpFilterText: '$COMMMIT_MESSAGE'

            )
        }

        // 常量参数，初始确定后一般不需更改
        environment {
            CRED_ID = "gitlab-token"
            // 声明全局变量
            DOCKER_GROUP = "a7179072/devopsdemo"
            DOCKER_IMAGE_TAG = ""
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
            stage('处理一些变量'){
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    script{
                        DOCKER_IMAGE_TAG = "${branchName}"
                    }
                }
            }
            stage('CI获取版本库代码') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    script{
                        anothertool.PrintMes("CI CODE PULL BEGIN","green")
                        method.gitscm("${branchName}","${url}")
                        anothertool.PrintMes("CI CODE PULL OVER","green")
                        // 覆盖配置文件
                        cfg = pipelineCfg()
                        println cfg
                    }
                }
            }

            stage('推送docker') {
                when { expression {
                    return  (runOpts == "auto")
                    }
                }
                steps {
                    container('jnlp-agent-docker') {
                        script{
                            docker.PushHarborDocker("${PROJECT}","${DOCKER_IMAGE_TAG}","${DOCKER_GROUP}")
                        }
                    }
                }
            }

            // stage('打包推送到helm') {
            //     when { expression {
            //         return  (runOpts == "auto")
            //         }
            //     }
            //     steps {
            //         container("${cfg.CI_EKS_NAME}") {
            //             script{
            //                 // dir("${JOB_NAME}-${BUILD_NUMBER}/${COMMMIT_MESSAGE}/deployment/helm/"){
            //                 dir("${COMMMIT_MESSAGE}/deployment/helm/"){
            //                     withCredentials([usernamePassword(credentialsId: 'harbor-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
            //                         sh """
            //                         HELM_PACKAGE_NAME=`helm package ${COMMMIT_MESSAGE}| awk -F '/' '{print \$NF}'`
            //                         helm repo add --username=${username} --password=${password}  ${COMMMIT_MESSAGE} https://${HARBOR_URL}/chartrepo/${GITLAB_GROUP}
            //                         helm push \$HELM_PACKAGE_NAME ${COMMMIT_MESSAGE}
            //                         """
            //                     }
            //                 }
            //             }
            //         }
            //     }
            // }


        }
    }
}