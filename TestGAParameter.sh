

cd /home/userfs/z/zs673/FIFOSpinLockFramework/result
pwd

rm -rf *.txt
cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup.out


javac $(find ./src/* | grep .java)

LD_LIBRARY_PATH=src nohup java -cp /home/userfs/z/zs673/FIFOSpinLockFramework/bin evaluationForSection5.TestGAParameter &