

cd /Users/ZS-MAC/Desktop/TestMrsPAnalysis/src

javac javaToC/MIPSolverC.java 
javah javaToC.MIPSolverC

cd /Users/ZS-MAC/Desktop/TestMrsPAnalysis/src/native

c++ -Wall -Wextra -Werror -Wno-unused-parameter  -Wno-deprecated        -fPIC -Iinclude -I/usr/local/include -I/usr/local/include  -DCONFIG_HAVE_GLPK -O2 -march=native   -c -o glpk.o src/linprog/glpk.cpp

c++ -Wall -Wextra -Werror -Wno-unused-parameter  -Wno-deprecated        -fPIC -Iinclude -I/usr/local/include -I/usr/local/include  -DCONFIG_HAVE_GLPK -O2 -march=native   -c -o lp_spinlocks.o src/blocking/linprog/lp_spinlocks.cpp
c++ -Wall -Wextra -Werror -Wno-unused-parameter  -Wno-deprecated        -fPIC -Iinclude -I/usr/local/include -I/usr/local/include  -DCONFIG_HAVE_GLPK -O2 -march=native   -c -o lp_spinlock_msrp.o src/blocking/linprog/lp_spinlock_msrp.cpp

c++ -Wall -Wextra -Werror -Wno-unused-parameter  -Wno-deprecated   -fPIC -Iinclude  -I/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/include -I/Library/Java/JavaVirtualMachines/jdk1.8.0_20.jdk/Contents/Home/include/darwin -I/Users/ZS-MAC/Desktop/TestMrsPAnalysis/src -I/usr/local/include -I/usr/local/include  -DCONFIG_HAVE_GLPK -O2 -march=native   -c -o testmain.o src/testmain.cpp

c++ -o ../libmipsolverc.dylib testmain.o tasks.o sharedres.o dpcp.o mpcp.o fmlp_plus.o global-fmlp.o msrp.o global-omlp.o part-omlp.o clust-omlp.o rw-phase-fair.o rw-task-fair.o msrp-holistic.o global-pip.o ppcp.o baker.o baruah.o gfb.o bcl.o bcl_iterative.o rta.o ffdbf.o gedf.o gel_pl.o load.o cpu_time.o qpa.o la.o sim.o schedule_sim.o lp_common.o io.o lp_dflp.o lp_dpcp.o lp_mpcp.o lp_fmlp.o lp_omip.o lp_spinlocks.o lp_spinlock_msrp.o lp_spinlock_unordered.o lp_spinlock_prio.o lp_spinlock_prio_fifo.o lp_gfmlp.o lp_global.o lp_global_pip.o lp_ppcp.o lp_sa_gfmlp.o lp_global_fmlpp.o lp_prsb.o lp_no_progress_priority.o lp_no_progress_fifo.o lp_global_no.o lp_global_pi.o lp_global_rsb.o lp_global_prio.o lp_global_fifo.o glpk.o -L/usr/local/lib -lgmp -lgmpxx -L/usr/local/lib -lglpk





