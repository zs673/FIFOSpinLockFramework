

cd /home/userfs/z/zs673/FIFOSpinLockFramework

#mkdir result
#cd result
#pwd
#rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup1.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.CompleteFramework 1 &> nohup1.out&
