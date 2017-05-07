rm -rf nohup.out
cd result
rm -rf *.txt
cd ..
ls

javac src/noAllocation/analysis/*.java 
src/noAllocation/entity/*.java \
src/noAllocation/geneticAlgoritmSolver/*.java \
src/noAllocation/basicAnalysis/*.java \
src/noAllocation/generatorTools/*.java \
src/noAllocation/test/*.java \
src/allocation/analysis/*.java \
src/allocation/entity/*.java \
src/allocation/geneticAlgoritmSolver/*.java \
src/allocation/basicAnalysis/*.java \
src/allocation/generatorTools/*.java \
src/allocation/test/*.java

nohup java -cp src/ noAllocation.test.GATestParallel
