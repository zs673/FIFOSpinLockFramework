

cd /home/userfs/z/zs673/FIFOSpinLockFramework

#mkdir result
#cd result
#pwd
#rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup422.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.CompleteSuccess 4 22 &> nohup422.out&
