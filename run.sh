rm -rf nohup.out
cd result
rm -rf *.txt
cd ..
ls

javac src/analysis/*.java src/entity/*.java src/geneticAlgoritmSolver/*.java src/basicAnalysis/*.java src/generatorTools/*.java src/test/*.java

nohup java -cp src/ test.GATestParallel
