package org.devops

def PushHarborDocker(docker_image_name,docker_image_tag,harbor_group) {
    withCredentials([usernamePassword(credentialsId: 'harbor-admin', passwordVariable: 'password', usernameVariable: 'username')]) {
        
        docker_image = "${docker_image_name}:${docker_image_tag}"
        sh """
        docker login -u ${username} -p ${password} 
        docker build -t ${docker_image} .
        docker tag ${docker_image} ${harbor_group}/${docker_image}
        docker push ${harbor_group}/${docker_image}
        """
    }
    return docker_image
}


