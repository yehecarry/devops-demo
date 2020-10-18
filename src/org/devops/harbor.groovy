//封装HTTP请求
def HttpReq(reqType,reqUrl,reqBody){
    def harborServer = "https://harbor.inf.funplus.social/api/v2.0/projects"
    withCredentials([usernamePassword(credentialsId: 'harbor-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
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

def test(){
  def project_name_list = []
  def conn ="https://harbor.tech.funplus.social/api/v2.0/projects"
  def parser = new JsonSlurper()
  def http = new URL(conn).openConnection()
  def auth_token = "admin:sqMDoOo5mFuI7VwI"; def basic_auth = "Basic ${auth_token.bytes.encodeBase64().toString()}"; 
  http.setRequestProperty("Authorization", basic_auth)
  http.setRequestMethod('GET')
  def getRC = http.getResponseCode()
  if(getRC.equals(200)) {
      res=http.getInputStream().getText()
      def json = parser.parseText(res)
      println json
      for (project in json){
      project_name_list += project.name
      }
      return project_name_list
} 