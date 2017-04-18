#include <iostream>

#include "tasks.h"
#include "sharedres.h"
#include "res_io.h"
#include "lp_analysis.h"

#include <jni.h>
#include "javaToC_MIPSolverC.h"

using namespace std;

JNIEXPORT jlong JNICALL Java_javaToC_MIPSolverC_solveMIP(JNIEnv *env, jobject solver, jobject tasks, jobject resources, jint TotalTasksSize, jboolean isPreemptable)
{
    jlong blocking = 10;

	// get arrayLists of resources and tasks and their size
    jclass arrayListClass = env->FindClass("java/util/ArrayList");
    jmethodID size = env->GetMethodID(arrayListClass, "size", "()I");
    jmethodID geti = env->GetMethodID(arrayListClass, "get", "(I)Ljava/lang/Object;");

    jint tasks_size = env->CallIntMethod(tasks, size);
    jint resources_size = env->CallIntMethod(resources, size);
//    cout << "partitions: " << tasks_size << "  resource size: " << resources_size << endl;

    if(tasks_size == 0 || resources_size == 0){
		cout << "No tasks or resources" << endl;
		return -1;
	}

	// get task and resource class and required fields.
	jclass resourceClass = env->FindClass("entity/Resource");
	jfieldID csl = env->GetFieldID(resourceClass, "csl", "J");

	jclass taskClass = env->FindClass("entity/SporadicTask");
	//jfieldID priority = env->GetFieldID(taskClass, "priority", "I");
	jfieldID period = env->GetFieldID(taskClass, "period", "J");
	jfieldID partition = env->GetFieldID(taskClass, "partition", "I");
	jfieldID WCET = env->GetFieldID(taskClass, "WCET", "J");
	jfieldID Ri = env->GetFieldID(taskClass, "Ri", "J");
	jfieldID local = env->GetFieldID(taskClass, "local", "J");
	jfieldID spin = env->GetFieldID(taskClass, "spin", "J");
	jfieldID total_blocking = env->GetFieldID(taskClass, "total_blocking", "J");

	jfieldID hasResource = env->GetFieldID(taskClass, "hasResource", "I");

	jfieldID resource_required_index = env->GetFieldID(taskClass, "resource_required_index_cpoy", "[I");
	jfieldID number_of_access_in_one_release = env->GetFieldID(taskClass, "number_of_access_in_one_release_copy", "[I");

	ResourceSharingInfo rsi(TotalTasksSize);

    for (int i = 0; i < tasks_size; i++)
    {
		jobject task_on_partition = env->CallObjectMethod(tasks, geti, i);
		jint tasks_on_partition_size = env->CallIntMethod(task_on_partition, size);

		for (int j = 0; j < tasks_on_partition_size; j++)
		{
			jobject task = env->CallObjectMethod(task_on_partition, geti, j);
			
			jint priority_value = /*env->GetIntField(task, priority)*/ j+1;
			jlong period_value = env->GetLongField(task, period);
			jint partition_value = env->GetIntField(task, partition);
			jlong Ri_value = env->GetLongField(task, Ri);
			jlong WCET_value = env->GetLongField(task, WCET);

			jint hasResource_value = env->GetIntField(task, hasResource);

			rsi.add_task(period_value, Ri_value, partition_value, priority_value, WCET_value);

//			cout << "task: " << priority_value << "   " << hasResource_value << endl;
			if(hasResource_value > 0){
				jobject resource_required_index_object = env->GetObjectField(task, resource_required_index);
				jintArray resource_required_index_value = (jintArray)resource_required_index_object;
				jsize resource_required_length = env->GetArrayLength(resource_required_index_value);
				jint *r_index = env->GetIntArrayElements(resource_required_index_value, 0);

				jobject number_of_access_in_one_release_object = env->GetObjectField(task, number_of_access_in_one_release);
				jintArray number_of_access_in_one_release_value = (jintArray)number_of_access_in_one_release_object;
				jint *num_access = env->GetIntArrayElements(number_of_access_in_one_release_value, 0);

				for (int k = 0; k < resource_required_length; k++)
				{
					jobject resource = env->CallObjectMethod(resources, geti, r_index[k]);
					jlong csl_value = env->GetLongField(resource, csl);

					rsi.add_request(r_index[k], num_access[k], csl_value);
				}
				env->ReleaseIntArrayElements(resource_required_index_value, r_index, 0);
				env->ReleaseIntArrayElements(number_of_access_in_one_release_value, num_access, 0);
			}

			env->DeleteLocalRef(task);
		}
		env->DeleteLocalRef(task_on_partition);
    }

//	cout << rsi << endl;
	BlockingBounds *results;

    if(!isPreemptable){
    	results = lp_pfp_msrp_bounds(rsi);
    }
    else{
    	results = lp_pfp_preemptive_fifo_spinlock_bounds(rsi);
    }

    int indexer = 0;
    for (int i = 0; i < tasks_size; i++)
        {
    		jobject task_on_partition = env->CallObjectMethod(tasks, geti, i);
    		jint tasks_on_partition_size = env->CallIntMethod(task_on_partition, size);

    		for (int j = 0; j < tasks_on_partition_size; j++)
    		{
    			jobject task = env->CallObjectMethod(task_on_partition, geti, j);
    			env->SetLongField(task, total_blocking, ((*results)[indexer].total_length));
    			env->SetLongField(task, spin, ((*results).get_remote_blocking(indexer)));
    			env->SetLongField(task, local, ((*results).get_arrival_blocking(indexer)));
    			indexer++;


    			env->DeleteLocalRef(task);
    		}
    		env->DeleteLocalRef(task_on_partition);
        }

    delete results;

    return blocking;
}

int aa();
JNIEXPORT void JNICALL Java_javaToC_MIPSolverC_helloFromC(JNIEnv *env, jobject obj)
{
    cout << "111hello from C side testmain! hahahah" << endl;
    aa();
}

int main(int argc, char **argv)
{
    aa();

    return 0;
}

int aa()
{
   ResourceSharingInfo rsi(100);
   unsigned int i;

   rsi.add_task(50000, 50000, 0, 2);
   rsi.add_request(0, 2, 1);

   rsi.add_task(30000, 30000, 0, 1);
   rsi.add_request(0, 4, 3);

   rsi.add_task(20000, 20000, 0, 0);
   rsi.add_request(0, 4, 1);

   rsi.add_task(50000, 50000, 1, 3);
   rsi.add_request(0, 2, 1);

   rsi.add_task(30000, 30000, 1, 2);
   rsi.add_request(0, 3, 3);
   rsi.add_request(1, 100, 100);

   rsi.add_task(20000, 20000, 1, 1);
   rsi.add_request(0, 3, 1);

   rsi.add_task(50000, 50000, 2, 2);
   rsi.add_request(0, 2, 1);

   rsi.add_task(30000, 30000, 2, 1);
   rsi.add_request(0, 5, 3);

   rsi.add_task(20000, 20000, 2, 0);
   rsi.add_request(0, 2, 1);

   rsi.add_task(3000, 3000, 3, 0);
   rsi.add_request(1, 1, 1);

   rsi.add_task(5000, 5000, 1, 0);

   rsi.add_task(100000, 100000, 4, 100);
   rsi.add_request(3, 3, 3);

   cout << rsi << endl;

   BlockingBounds *results;

   results = lp_pfp_msrp_bounds(rsi);

   cout << endl
	 << endl
	 << "MSRP (LP)" << endl;
   for (i = 0; i < results->size(); i++)
	cout << "T" << i
	     << " y=" << rsi.get_tasks()[i].get_priority()
	     << " c=" << rsi.get_tasks()[i].get_cluster()
	     << ": total=" << (*results)[i].total_length
	     << "  remote=" << results->get_remote_blocking(i)
	     << "  local=" << results->get_local_blocking(i)
	     << endl;

   delete results;

   results = lp_pfp_preemptive_fifo_spinlock_bounds(rsi);

   cout << endl
	 << endl
	 << "Preemptive MSRP (LP)" << endl;
   for (i = 0; i < results->size(); i++)
	cout << "T" << i
	     << " y=" << rsi.get_tasks()[i].get_priority()
	     << " c=" << rsi.get_tasks()[i].get_cluster()
	     << ": total=" << (*results)[i].total_length
	     << "  remote=" << results->get_remote_blocking(i)
	     << "  local=" << results->get_local_blocking(i)
	     << endl;

   delete results;
    return 0;
}
