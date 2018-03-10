

cd /home/userfs/z/zs673/FIFOSpinLockFramework

mkdir resultCrossover
cd resultCrossover
pwd
rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm resultCrossover.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection5.TestCrossover &> resultCrossover.out&
