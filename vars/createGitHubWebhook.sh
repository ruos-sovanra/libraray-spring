def createGitHubWebhook(String githubToken, String repoUrl) {
    // Configuration
    def webhookUrl = 'https://jenkins.psa-khmer.world/github-webhook/'  // Replace with your webhook URL
    def webhookSecret = '1102b43d4bae5e52ede6fc05ee5dc20e91'  // Replace with your webhook secret
    def webhookEvents = '["push"]'  // Customize the events you want

    // Extract GITHUB_USER_OR_ORG and REPO_NAME from the URL using regex
    def userOrg, repoName
    if (repoUrl =~ /github\.com[:\/](.+)\/(.+)\.git/) {
        userOrg = repoUrl.replaceFirst(/github\.com[:\/](.+)\/(.+)\.git/, '$1')
        repoName = repoUrl.replaceFirst(/github\.com[:\/](.+)\/(.+)\.git/, '$2')
    } else if (repoUrl =~ /github\.com[:\/](.+)\/(.+)/) {
        userOrg = repoUrl.replaceFirst(/github\.com[:\/](.+)\/(.+)/, '$1')
        repoName = repoUrl.replaceFirst(/github\.com[:\/](.+)\/(.+)/, '$2')
    } else {
        error('Invalid GitHub URL format. Please ensure the URL is correct.')
    }

    // Function to create webhook for the specific repository
    def createWebhook = { repo ->
        echo "Creating webhook for ${repo}"

        def response = httpRequest(
            url: "https://api.github.com/repos/${repo}/hooks",
            httpMode: 'POST',
            contentType: 'APPLICATION_JSON',
            customHeaders: [[name: 'Authorization', value: "token ${githubToken}"]],
            requestBody: """{
                "name": "web",
                "active": true,
                "events": ${webhookEvents},
                "config": {
                    "url": "${webhookUrl}",
                    "content_type": "json",
                    "secret": "${webhookSecret}",
                    "insecure_ssl": "0"
                }
            }"""
        )

        // Check if the webhook creation was successful
        if (response.status == 201) {
            echo "Webhook successfully created for ${repo}"
        } else {
            error("Failed to create webhook for ${repo}: ${response.content}")
        }
    }

    // Create webhook for the specific repository
    def fullRepoName = "${userOrg}/${repoName}"
    createWebhook(fullRepoName)

    echo "Webhook setup process completed for ${fullRepoName}!"
}