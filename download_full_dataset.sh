#!/bin/bash
# Create directory
mkdir gh_data
cd gh_data

# Download data from 01-01-2024 to 03-01-2024 (72 total hours)
wget -nc https://data.gharchive.org/2024-01-{01..03}-{0..23}.json.gz