

cd /home/userfs/z/zs673/FIFOSpinLockFramework

mkdir resultCombinedWFDM
cd resultCombinedWFDM
pwd

rm -rf *.txt
cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm nohupCombinedWFDM.out


javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection6.ProtocolsCombinedWFDM &> nohupCombinedWFDM.out&
