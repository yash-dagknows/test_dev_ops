pipeline {
    agent any

    environment {
        PROJECT_ID = 'caramel-hallway-365911' // Your GCP project ID
        REPOSITORY_NAME = 'yash-artifact-registry-test' // Your Artifact Registry repository name
        GCR_URL = "asia-south1-docker.pkg.dev/${PROJECT_ID}/${REPOSITORY_NAME}"
        GITHUB_REPO = 'yash-dagknows/test_dev_ops' // Your GitHub repository
        GITHUB_CREDENTIALS = 'yash-dagknows-github-pat' // Jenkins credential ID for GitHub
        GCR_CREDENTIALS = 'Yash_GCP_Service_Account_Test' // Jenkins credential ID for Google Cloud
    }

    stages {
        stage('SCM Checkout') {
            steps {
                git branch: 'main', url: "https://github.com/${GITHUB_REPO}.git", credentialsId: "${GITHUB_CREDENTIALS}"
            }
        }
        stage('Build Docker Image') {
            steps {
                script {
                    sh """
                    echo "Building Docker image..."
                    docker build -t ${GCR_URL}:${BUILD_NUMBER} .
                    """
                }
            }
        }
        stage('Push to GCR') {
            steps {
                withCredentials([file(credentialsId: 'gcr-service-account-key', variable: 'GOOGLE_APPLICATION_CREDENTIALS')]) {
                    script {
                        sh """
                        echo "Authenticating with Google Cloud..."
                        gcloud auth activate-service-account --key-file=${GOOGLE_APPLICATION_CREDENTIALS}
                        gcloud auth configure-docker asia-south1-docker.pkg.dev

                        echo "Pushing Docker image to GCR..."
                        docker push ${GCR_URL}:${BUILD_NUMBER}
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
