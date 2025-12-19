#!/bin/bash
# Create directory
mkdir gh_data
cd gh_data

# Download data from 01-01-2024 to 01-01-2024 00:00-00:59 (1 total hours)
wget -nc https://data.gharchive.org/2024-01-01-0.json.gz