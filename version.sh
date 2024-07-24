#!/usr/bin/env bash

# Check if an argument is provided
if [ -z "$1" ]; then
    echo "No image tag provided as input."
    exit 1
fi

image_tag=$1

# Increment the last numeric part of the image tag
image_tag_new=$(echo "$image_tag" | awk -F. -v OFS=. '{$NF += 1 ; print}')

# Check if the new tag has been successfully generated
if [ -z "$image_tag_new" ]; then
    echo "Failed to generate new image tag from $image_tag."
    exit 1
else
    echo "$image_tag_new"
fi
