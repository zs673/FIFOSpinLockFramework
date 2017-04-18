cd /usr/userfs/z/zs673

cd /usr/userfs/z/zs673/TestMrsPAnalysis/result
rm -rf *.txt
cd /usr/userfs/z/zs673/TestMrsPAnalysis




bigset=0
for bigset in 3 4
	do
		for smallset in 1 2 3 4 5 6 7 8 9
   			do
   				LD_LIBRARY_PATH=src nohup java -cp /usr/userfs/z/zs673/TestMrsPAnalysis/bin -Djava.library.path="/usr/userfs/z/zs673/TestMrsPAnalysis/src" test.SchedulabilityTest 1 ${bigset} ${smallset} &
    	done
done

for bigset in 1 2 3
	do
		for smallset in 1 2 3 4 5
 			do
  				LD_LIBRARY_PATH=src nohup java -cp /usr/userfs/z/zs673/TestMrsPAnalysis/bin -Djava.library.path="/usr/userfs/z/zs673/TestMrsPAnalysis/src" test.SchedulabilityTest 2 ${bigset} ${smallset} &
   	done
done

for bigset in 2 3
	do
		for smallset in 1 2 3 4 5 6 7 8 9 10
  			do
 				LD_LIBRARY_PATH=src nohup java -cp /usr/userfs/z/zs673/TestMrsPAnalysis/bin -Djava.library.path="/usr/userfs/z/zs673/TestMrsPAnalysis/src" test.SchedulabilityTest 3 ${bigset} ${smallset} &
 	done
done
