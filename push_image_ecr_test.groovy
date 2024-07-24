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
                }
            }
        }

        stage('Check out source code') {
            steps {
                script {
                    withCredentials([usernamePassword(credentialsId: 'yash-dagknows-github-pat', usernameVariable: 'USERNAME', passwordVariable: 'GIT_TOKEN')]) {
                        sh """
                        git clone -b $Branch https://"\$GIT_TOKEN":x-oauth-basic@github.com/yash-dagknows/test_dev_ops.git
                        cd $APP_DIR
                        """
                    }
                }
            }
        }

        stage('Initialize pipeline') {
            steps {
                script {
                    account_id = '188379622596'
                    aws_region = 'us-east-2'
                    aws_role = 'JenkinsBuildRole'
                }
            }
        }

        stage('Build') {
            steps {
                withAWS(roleAccount: "${account_id}", role: "${aws_role}") {
                    script {
                        sh """#!/bin/bash
                        export image_tag=\$(aws ssm get-parameter --name "/test_dev_ops/successful-build" --with-decryption --output text --query Parameter.Value)
                        export image_tag_new=\$(sh version.sh \$image_tag)
                        echo \$image_tag_new
                        cd $APP_DIR
                        /kaniko/executor --context $APP_DIR --dockerfile=Dockerfile --force --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:\$image_tag_new --destination=public.ecr.aws/n5k3t9x2/test_dev_ops:latest --single-snapshot --cache=false --cache-ttl=1h
                        aws ssm put-parameter --name "/test_dev_ops/successful-build" --type 'String' --value \$image_tag_new --overwrite
                        """
                    }
                }
            }
        }
    }
}