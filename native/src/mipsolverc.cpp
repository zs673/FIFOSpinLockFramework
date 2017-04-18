

#include <jni.h>
#include <iostream>
#include "javaToC_MIPSolverC.h"


void aa();
JNIEXPORT void JNICALL Java_javaToC_MIPSolverC_helloFromC(JNIEnv *env, jobject obj)
{
  std::cout<<"111hello from C side! hahahah"<<std::endl;
  aa();
}


void aa(){
  std::cout<<" in aa"<<std::endl;
}

