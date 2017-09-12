#!/bin/bash
for i in {1..100}
do
   echo "Running iteration $i"
   sbt run > $i.txt
done