

cd /home/userfs/z/zs673/FIFOSpinLockFramework/result
pwd

rm -rf *.txt
cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection5.TestGeneration &> nohup2.out&
