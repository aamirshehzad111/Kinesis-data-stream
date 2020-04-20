#!bin/bash
odd_number=(1 3 5 7 9 11 13 15 17 19 21 23 25 27 29 33 55 67)
for i in ${odd_number[@]}
do
  aws kinesis put-record --stream-name number-generator-stream  --data=$i, --partition-key=oddKey
done
