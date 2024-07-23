pipeline {
    agent any

    environment {
        PROJECT_ID = 'caramel-hallway-365911'
        REPOSITORY_NAME = 'yash-artifact-registry-test'
        GCR_URL = "asia-south1-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY_NAME}"
        GITHUB_REPO = 'yash-dagknows/test_dev_ops'
        GITHUB_CREDENTIALS = 'yash-dagknows-github-pat'
        GCR_CREDENTIALS = 'Yash_GCP_Service_Account_Test'
    }

    stages {
        stage('Install Docker and gcloud') {
            steps {
                script {
                    sh '''
                    echo "Installing Docker..."
                    sudo apt-get update
                    sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release
                    curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo apt-key add -
                    sudo add-apt-repository "deb [arch=amd64] https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable"
                    sudo apt-get update
                    sudo apt-get install -y docker-ce docker-ce-cli containerd.io

                    echo "Starting Docker..."
                    sudo service docker start

                    echo "Installing gcloud..."
                    echo "deb [signed-by=/usr/share/keyrings/cloud.google.gpg] http://packages.cloud.google.com/apt cloud-sdk main" | sudo tee -a /etc/apt/sources.list.d/google-cloud-sdk.list
                    curl https://packages.cloud.google.com/apt/doc/apt-key.gpg | sudo apt-key --keyring /usr/share/keyrings/cloud.google.gpg add -
                    sudo apt-get update && sudo apt-get install -y google-cloud-sdk
                    '''
                }
            }
        }
        stage('Verify Tools') {
            steps {
                script {
                    sh 'docker --version'
                    sh 'gcloud --version'
                }
            }
        }
        stage('SCM Checkout') {
            steps {
                git branch: 'main', url: "https://github.com/${GITHUB_REPO}.git", credentialsId: "${GITHUB_CREDENTIALS}"
            }
        }
        stage('Authenticate with Google Cloud') {
            steps {
                withCredentials([file(credentialsId: 'gcr-service-account-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    script {
                        sh '''
                        echo "Authenticating with Google Cloud..."
                        gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
                        gcloud auth configure-docker asia-south1-docker.pkg.dev
                        '''
                    }
                }
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    sh '''
                    echo "Building Docker image..."
                    docker build -t ${GCR_URL}:${BUILD_NUMBER} .
                    '''
                }
            }
        }
        stage('Push to GCR') {
            steps {
                script {
                    sh '''
                    echo "Pushing Docker image to GCR..."
                    docker push ${GCR_URL}:${BUILD_NUMBER}
                    '''
                }
            }
        }
    }

    post {
        always {
            node('') {
                cleanWs()
            }
        }
    }
}
