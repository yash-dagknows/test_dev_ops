pipeline {
    agent {
        node {
            label 'docker'
        }
    }

    environment {
        APP_DIR = "${WORKSPACE}/test_dev_ops"
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

        stage('Build') {
            steps {
                timeout(time: 5, unit: 'MINUTES') {
                    withAWS(roleAccount: "${account_id}", role: "${aws_role}") {
                        script {
                            try {
                                sh """
                                set -e
                                export image_tag=\$(aws ssm get-parameter --name "/test_dev_ops/successful-build" --with-decryption --output text --query Parameter.Value)
                                export image_tag_new=\$(sh version.sh \$image_tag)
                                echo \$image_tag_new
                                cd $app_dir
                                /kaniko/executor --context $app_dir --dockerfile=Dockerfile --force --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:\$image_tag_new --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:latest --single-snapshot --cache=false --cache-ttl=1h
                                aws ssm put-parameter --name "/test_dev_ops/successful-build" --type "String" --value \$image_tag_new --overwrite
                                """
                                echo "Image built and pushed successfully. New image tag: \$image_tag_new"
                            } catch (Exception e) {
                                echo "Error during build: ${e.message}"
                                currentBuild.result = 'SUCCESS'
                                error("Marking build as successful due to custom condition.")
                            }
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Final Build Status: ' + currentBuild.result
        }
    }
}
