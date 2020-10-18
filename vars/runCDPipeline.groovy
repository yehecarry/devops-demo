#!groovy
def call() {
    // 导入封装的方法
    def method = new org.encapsulation.method()
    // 导入默认配置
    method.defaultConf()

    node('jenkins-default-agent'){
        stage('获取配置文件') {
            checkout scm
        }
        def cfg = pipelineCfg()
        println cfg
        switch(cfg.CD_TYPE) {
            case "DEMO":
                DEMO_CD_PIPELINE(cfg)
                break
        }
    }
}