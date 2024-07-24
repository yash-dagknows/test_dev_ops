#!/usr/bin/env bash

image_tag=$1

image_tag_new=$(echo "$image_tag" | awk -F. -v OFS=. '{$NF += 1 ; print}')

echo "$image_tag_new"
