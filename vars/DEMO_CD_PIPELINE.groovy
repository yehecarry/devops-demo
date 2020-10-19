#!groovy

@NonCPS
def get_userid() {
    // userId = "auto"
    def job = Jenkins.getInstance().getItemByFullName(env.JOB_NAME, Job.class)
    def build = job.getBuildByNumber(env.BUILD_ID as int)
    try{
        def userId = build.getCause(Cause.UserIdCause).getUserId()
    } catch (Exception e) {
        return "auto deploy"
    }
    def userId = build.getCause(Cause.UserIdCause).getUserId()
    return userId
}


def call(cfg) {
    // 导入封装的方法
    def method = new org.encapsulation.method()
    // 导入docker模块
    def docker = new org.devops.docker()
    // 导入sonar模块
    // def sonar = new org.devops.sonar()
    // 导入jira模块
    // def jira = new org.devops.jira()
    // 导入通用模块
    def anothertool = new org.devops.anothertool()
    // 获取用户名
    def BUILD_USER = get_userid()

    // 导入默认配置
    method.defaultConf()

    properties([
        parameters([
            [$class: 'ChoiceParameter', 
                choiceType: 'PT_SINGLE_SELECT', 
                description: '项目组所在namespace',  
                name: 'NAME_SPACE', 
                randomName: 'choice-parameter-1', 
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [
                        classpath: [], 
                        sandbox: false, 
                        script: 
                            'return[\'刷新网页\']'
                    ], 
                    script: [
                        classpath: [], 
                        sandbox: false,
                        script: 
                            return ["demo"]
                    ]
                ]
            ], 
            [$class: 'CascadeChoiceParameter', 
                choiceType: 'PT_SINGLE_SELECT', 
                description: '请选择Harbor中DockerImage的名字',
                name: 'DOCKER_IMAGE', 
                randomName: 'choice-parameter-2', 
                referencedParameters: 'NAME_SPACE', 
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [
                        classpath: [], 
                        sandbox: false, 
                        script: 
                            'return[\'没有选择任何的项目\']'
                    ], 
                    script: [
                        classpath: [], 
                        sandbox: false, 
                        script: 
                            '''import groovy.json.JsonSlurper
							def Get_env() {
							  def project_name_list = []
							  def conn ="https://hub.docker.com/v2/repositories/a7179072/"
							  def parser = new JsonSlurper()
							  def http = new URL(conn).openConnection()
							  http.setRequestMethod('GET')
							  def getRC = http.getResponseCode()
							  if(getRC.equals(200)) {
								res=http.getInputStream().getText()
								def json = parser.parseText(res)["results"]
								for (project in json){
								  println project.name
								  project_name_list += project.name
								}
								return project_name_list
							  }
							}
							Get_env()'''
                    ]
                ]
            ],
            [$class: 'CascadeChoiceParameter', 
                choiceType: 'PT_SINGLE_SELECT', 
                description: '请选择Harbor中Docker的TAG', 
                name: 'GROUPA_DOCKER_TAG',
                randomName: 'choice-parameter-3', 
                referencedParameters: 'NAME_SPACE,DOCKER_IMAGE',
                script: [
                    $class: 'GroovyScript', 
                    fallbackScript: [
                        classpath: [], 
                        sandbox: false, 
                        script: 
                            'return[\'没有选择任何的项目\']'
                    ], 
                    script: [
                        classpath: [], 
                        sandbox: false, 
                        script: 
							'''import groovy.json.JsonSlurper
							def Get_env() {
							  def project_name_list = []
							  def conn ="https://hub.docker.com/v2/repositories/a7179072/demo/tags/"
							  def parser = new JsonSlurper()
							  def http = new URL(conn).openConnection()
							  http.setRequestMethod('GET')
							  def getRC = http.getResponseCode()
							  if(getRC.equals(200)) {
								res=http.getInputStream().getText()
								def json = parser.parseText(res)["results"]
								for (project in json){
								  println project.name
								  project_name_list += project.name
								}
								return project_name_list
							  }
							}
							Get_env()'''
                            '''
                    ]
                ]
            ]
        ])
    ])
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
            disableConcurrentBuilds() //禁止并行
            timeout(time: 1, unit: 'HOURS')  //流水线超时设置1h
        }
        



        // 常量参数，初始确定后一般不需更改
        // environment {
        // }

        post {
            success {
                script{
                    currentBuild.description = "\n 构建成功!" 
                }
            }


            failure {
                script{
                    currentBuild.description = "\n 构建失败!" 
                }
            }
            
            aborted {
                script{
                    currentBuild.description = "\n 构建取消!" 
                }
            }

            always {
                cleanWs()
            }
        }

        //步骤设置
        stages {
            
            // stage('确认发版') {
            //     input {
            //         message "您发版的项目为${Release_Project},是否继续?"
            //         ok '确定'
            //     }
            //     steps{
            //         println Release_Project
            //     }   
            // }

            stage('部署服务') {
                steps {
                    container("${cfg.CI_EKS_NAME}") {
                        script{
                            withCredentials([usernamePassword(credentialsId: 'harbor-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
                                sh """
                                aws eks --region ${REGION} update-kubeconfig --name ${EKS_NAME}
                                helm repo add --username=${username} --password=${password} ${DOCKER_IMAGE} https://xxxxx/chartrepo/${NAME_SPACE}
                                helm repo update
                                helm upgrade -i -n ${NAME_SPACE} --set server.GroupA.image.tag=${GROUPA_DOCKER_TAG} --set server.GroupB.image.tag=${GROUPB_DOCKER_TAG} --set server.env[0].name="SPRING_ENV",server.env[0].value="tw_prod" --set filebeat.config.env="tw_prod" ${DOCKER_IMAGE} ${DOCKER_IMAGE}/${DOCKER_IMAGE}
                                """
                                // helm upgrade -i -n ${NAME_SPACE} --set server.GroupA.image.tag=${GROUPA_DOCKER_TAG} --set server.GroupB.image.tag=${GROUPB_DOCKER_TAG} --set server.env[0].name="SPRING_ENV",server.env[0].value="tw_test123" ${DOCKER_IMAGE} ${DOCKER_IMAGE}/${DOCKER_IMAGE}
                            }
                        }
                    }
                }
            }
            
        }
    }
}