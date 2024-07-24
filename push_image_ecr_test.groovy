pipeline {
    agent {
        node {
            label 'docker'  // Ensure this node is properly set up for Docker operations.
        }
    }

    environment {
        APP_DIR = "${WORKSPACE}/test_dev_ops"  // Directory change to align with the GitHub repository structure.
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    properties([
                        parameters([
                            string(defaultValue: 'main', description: '<b>Select the desired branch. Ensure the branch name matches with the git branch.</b><br><br>', name: 'Branch'),
                        ])
                    ])
                    echo "Initialization complete"
                }
            }
        }

        stage('Check out source code') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'yash-dagknows-github-pat', usernameVariable: 'USERNAME', passwordVariable: 'GIT_TOKEN')]) {
                        sh """
                        set -e
                        git clone -b $Branch https://"\$GIT_TOKEN":x-oauth-basic@github.com/yash-dagknows/test_dev_ops.git
                        cd $env.app_dir
                        """
                    }
                    echo "Source code checked out successfully"
                }
            }
        }

        stage('Initialize pipeline') {
            steps {
                script {
                    account_id = '188379622596'
                    aws_region = 'us-east-2'
                    aws_role = 'JenkinsBuildRole'
                    echo "Pipeline variables initialized"
                }
            }
        }

        stage('Debug Environment') {
            steps {
                script {
                    sh "echo PATH is $PATH"
                    sh "which nohup || echo 'nohup not found'"
                }
            }
        }

        stage('Build') {
            steps {
                withAWS(roleAccount: "${account_id}", role: "${aws_role}") {
                    script {
                        sh """
                        set -e
                        #!/bin/bash
                        export image_tag=\$(aws ssm get-parameter --name "/test_dev_ops/successful-build" --with-decryption --output text --query Parameter.Value)
                        export image_tag_new=\$(sh version.sh \$image_tag)
                        echo \$image_tag_new
                        cd $app_dir
                        /kaniko/executor --context $env.app_dir --dockerfile=Dockerfile --force --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:\$image_tag_new --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:latest --single-snapshot --cache=false --cache-ttl=1h
                        aws ssm put-parameter --name "/test_dev_ops/successful-build" --type "String" --value \$image_tag_new --overwrite
                        echo "AWS SSM parameter updated successfully."
                        exit 0
                        """
                        echo "Image built and pushed successfully. New image tag: \$image_tag_new"
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Pipeline execution is now complete.'
        }
    }
}
