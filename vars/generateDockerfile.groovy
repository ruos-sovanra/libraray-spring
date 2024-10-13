def call(String projectPath) {
    def projectType = detectProjectType(projectPath)

    if (projectType) {
        echo "Detected project type: ${projectType}"

        if (!dockerfileExists(projectPath)) {
            def packageManager = detectPackageManager(projectPath)
            writeDockerfile(projectType, projectPath, packageManager)
        } else {
            echo "Dockerfile already exists at ${projectPath}/Dockerfile, skipping generation."
        }

        return projectType
    } else {
        error "Unable to detect the project type for ${projectPath}."
    }
}

def dockerfileExists(String projectPath) {
    return fileExists("${projectPath}/Dockerfile")
}

def detectProjectType(String projectPath) {
    if (fileExists("${projectPath}/package.json")) {
        def packageJson = readJSON file: "${projectPath}/package.json"
        if (packageJson.dependencies?.'next') {
            return 'nextjs'
        } else if (packageJson.dependencies?.'react') {
            return 'react'
        }
    } else if (fileExists("${projectPath}/pom.xml")) {
        return 'springboot-maven'
    } else if (fileExists("${projectPath}/build.gradle")) {
        return 'springboot-gradle'
    } else if (fileExists("${projectPath}/pubspec.yaml")) {
        return 'flutter'
    }

    return null
}

def detectPackageManager(String projectPath) {
    if (fileExists("${projectPath}/package-lock.json")) {
        return 'npm'
    } else if (fileExists("${projectPath}/yarn.lock")) {
        return 'yarn'
    } else if (fileExists("${projectPath}/pnpm-lock.yaml")) {
        return 'pnpm'
    } else if (fileExists("${projectPath}/bun.lockb")) {
        return 'bun'
    }
    return 'npm'
}

def writeDockerfile(String projectType, String projectPath, String packageManager) {
    try {
        def dockerfileContent = libraryResource "dockerfileTemplates/Dockerfile-${projectType}"
        dockerfileContent = dockerfileContent.replaceAll("\\{\\{packageManager\\}\\}", packageManager)
        writeFile file: "${projectPath}/Dockerfile", text: dockerfileContent
        echo "Dockerfile successfully written for ${projectType} project at ${projectPath}/Dockerfile"
    } catch (Exception e) {
        error "Failed to write Dockerfile for ${projectType} project: ${e.message}"
    }
}

def pushDockerImage(String dockerImageName, String dockerImageTag, String credentialsId) {
    try {
        withCredentials([usernamePassword(credentialsId: credentialsId, passwordVariable: 'DOCKER_PASS', usernameVariable: 'DOCKER_USER')]) {
            def dockerHubRepo = "${DOCKER_USER}/${dockerImageName}:${dockerImageTag}"

            // Docker login
            sh """
            docker login -u ${DOCKER_USER} -p ${DOCKER_PASS}
            """

            // Tag the image for the registry
            sh """
            docker tag ${dockerImageName}:${dockerImageTag} ${dockerHubRepo}
            """

            // Push the image to Docker Hub
            sh """
            docker push ${dockerHubRepo}
            """

            echo "Image pushed to Docker Hub: ${dockerHubRepo}"
        }
    } catch (Exception e) {
        error "Failed to push Docker image: ${e.message}"
    }
}
