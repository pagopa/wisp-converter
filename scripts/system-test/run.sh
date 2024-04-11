#!/bin/bash

cd ./src || exit
npm install
clear
npm run test:"$1" "$2" "$3"