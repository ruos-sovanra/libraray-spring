def call(String subdomain, String domain, String deployPort) {
    // Ensure required variables are provided and non-empty
    if (!subdomain?.trim() || !domain?.trim() || !deployPort?.trim()) {
        error "subdomain, domain, and deployPort must be provided and cannot be empty"
    }

    echo "Subdomain: ${subdomain}"
    echo "Domain: ${domain}"
    echo "Deploy Port: ${deployPort}"

    def configFilePath = "/etc/nginx/sites-available/${subdomain}.${domain}"

    // Check if the config file already exists
    def configExists = sh(script: "if [ -f ${configFilePath} ]; then echo 'exists'; else echo 'not exists'; fi", returnStdout: true).trim()

    if (configExists == 'exists') {
        echo "Configuration for ${subdomain}.${domain} already exists. Skipping creation."
        return
    }

    // Load the Nginx template file
    def templateFile = libraryResource 'nginx Templates/configNginx.template'

    // Replace placeholders in the template
    def configContent = templateFile.replace('${domain}', domain)
                                    .replace('${subdomain}', subdomain)
                                    .replace('${deployPort}', deployPort)

    // Write the configuration to the file
    writeFile file: configFilePath, text: configContent

    // Debug step: print out the variables to ensure they are not empty
    echo "Generated Nginx Config:"
    echo configContent

    // Using Groovy variable interpolation inside the shell block
    sh """
    #!/bin/bash

    # Create a symlink to enable the site in Nginx
    ln -sf ${configFilePath} /etc/nginx/sites-enabled/${subdomain}.${domain}

    # Test Nginx configuration and reload Nginx
    nginx -t && systemctl reload nginx

    echo "Nginx configuration for ${subdomain}.${domain} has been created and deployed."
    """

    sh """
    #!/bin/bash

    # Request a certificate for the domain
    certbot --nginx -d ${subdomain}.${domain}

    # Test Nginx configuration and reload Nginx
    nginx -t && systemctl reload nginx

    echo "SSL certificate for ${subdomain}.${domain} has been created and deployed."
    """

}
