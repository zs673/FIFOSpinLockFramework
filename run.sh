rm -rf nohup.out
cd result
rm -rf *.txt
cd ..
ls

javac $(find ./src/* | grep .java)

#nohup java -cp src/ noAllocation.test.GATestParallel

nohup java -cp src/ test.StaticTest
