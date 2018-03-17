

cd /home/userfs/z/zs673/FIFOSpinLockFramework

#mkdir result
#cd result
#pwd
#rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohup26.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.ProtocolsCombinedWFDMSuccessF 2 6 &> nohup26.out&
