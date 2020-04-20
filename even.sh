#!bin/bash
even_number=(2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 32 34)
for i in ${even_number[@]}
do
  aws kinesis put-record --stream-name number-generator-stream  --data=$i, --partition-key=evenKey
done
