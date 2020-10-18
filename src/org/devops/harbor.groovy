//封装HTTP请求
def HttpReq(reqType,reqUrl,reqBody){
    def harborServer = "https://hub.docker.com/"
    withCredentials([usernamePassword(credentialsId: 'docker-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
      result = httpRequest customHeaders: [[maskValue: true, name: 'PRIVATE-TOKEN', value: "${gitlabToken}"]], 
                httpMode: reqType, 
                contentType: "APPLICATION_JSON",
                consoleLogResponseBody: true,
                ignoreSslErrors: true, 
                requestBody: reqBody,
                url: "${harborServer}/${reqUrl}"
                //quiet: true
    }
    return result
}