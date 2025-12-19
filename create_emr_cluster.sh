#!/bin/bash

# -------------------------------------------------------------------------
# SET ENVIRONMENT VARIABLES FOR AWS CREDENTIALS AND CONFIGURATION
# -------------------------------------------------------------------------

export AWS_SHARED_CREDENTIALS_FILE=aws/credentials
export AWS_CONFIG_FILE=aws/config

# -------------------------------------------------------------------------
# INPUT VALIDATION AND VARIABLE SETUP
# -------------------------------------------------------------------------

# 1. Checks if the mandatory KEY_PAIR_NAME argument ($1) was provided.
if [ -z "$1" ]; then
    echo "Error: You must pass the EC2 key pair name (KEY_PAIR_NAME) as the first argument." >&2
    echo "Usage: $0 <KEY_NAME>" >&2
    exit 1
fi

KEY_PAIR_NAME=$1
AWS_REGION="us-east-1"
CLUSTER_NAME="Big Data Cluster"

echo "Starting EMR cluster creation..."
echo "Region: $AWS_REGION"
echo "Key Pair Name: $KEY_PAIR_NAME"

# -------------------------------------------------------------------------
# AWS EMR COMMAND EXECUTION
# -------------------------------------------------------------------------

# Execute the cluster creation command and capture the JSON output.
# The command is enclosed in $() for execution and variable assignment.
CLUSTER_INFO=$(
    aws emr create-cluster \
        --name "$CLUSTER_NAME" \
        --release-label "emr-7.11.0" \
        --applications Name=Hadoop Name=Spark \
        --instance-groups InstanceGroupType=MASTER,InstanceCount=1,InstanceType=m4.large InstanceGroupType=CORE,InstanceCount=2,InstanceType=m4.large \
        --service-role EMR_DefaultRole \
        --ec2-attributes InstanceProfile=EMR_EC2_DefaultRole,KeyName="$KEY_PAIR_NAME" \
        --region "$AWS_REGION" 
)

# -------------------------------------------------------------------------
# ERROR CHECKING AND ID EXTRACTION
# -------------------------------------------------------------------------

# Check the exit code of the 'aws' command. $? is 0 if successful.
if [ $? -ne 0 ]; then
    echo "Error executing the AWS EMR command. Check AWS configuration or parameters." >&2
    echo "AWS Output/Error: $CLUSTER_INFO" >&2
    exit 1
fi

# Parse the JSON output using string manipulation to extract the ClusterId.
CLUSTER_ID=$(echo "$CLUSTER_INFO" | grep '"ClusterId"' | cut -d ':' -f 2 | tr -d '", ' | head -1)

if [ -z "$CLUSTER_ID" ]; then
    echo "Parsing error: Could not find ClusterId in the AWS output." >&2
    echo "Full output received:"
    echo "$CLUSTER_INFO"
    exit 1
fi

# Return the Cluster ID as the final output.
echo "EMR Cluster created successfully."
echo "Cluster ID: "
echo "$CLUSTER_ID" 
exit 0