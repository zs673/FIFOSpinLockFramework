rm -rf nohup.out
cd result
rm -rf *.txt
cd ..
ls

nohup java -cp bin/ test.GATestParallel
