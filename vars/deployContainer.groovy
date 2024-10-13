def call(String containerName, String imageName, String imageTag, String deployPort) {
    sh """
        #!/bin/bash
        # Check if the container is running and stop it
        if [ \$(docker ps -q -f name=${containerName}) ]; then
            echo "Stopping running container ${containerName}..."
            docker stop ${containerName}
        fi

        # Remove the container if it exists
        if [ \$(docker ps -a -q -f name=${containerName}) ]; then
            echo "Removing container ${containerName}..."
            docker rm ${containerName}
        fi

        # Start a new container
        docker run -d --name ${containerName} -p ${deployPort}:3000 ${imageName}:${imageTag}
    """
}
