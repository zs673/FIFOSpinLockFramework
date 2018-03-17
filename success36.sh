

cd /home/userfs/z/zs673/FIFOSpinLockFramework

#mkdir result
#cd result
#pwd
#rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup36.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.ProtocolsCombinedWFDMSuccessF 3 6 &> nohup36.out&
