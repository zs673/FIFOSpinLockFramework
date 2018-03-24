

cd /home/userfs/z/zs673/FIFOSpinLockFramework

#mkdir result
#cd result
#pwd
#rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup21.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.CompleteSuccess 2 1 &> nohup21.out&
