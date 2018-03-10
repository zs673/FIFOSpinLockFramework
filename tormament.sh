

cd /home/userfs/z/zs673/FIFOSpinLockFramework

mkdir resultTourmament
cd resultTourmament
pwd
rm -rf *.txt

cd /home/userfs/z/zs673/FIFOSpinLockFramework
pwd
rm resultTourmament.out

javac $(find ./src/* | grep .java)

nohup java -cp src/ evaluationSection5.TestToumament &> resultTourmament.out&
